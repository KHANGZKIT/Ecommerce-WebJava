package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.config.KafkaProducerConfig;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.event.OrderConfirmedEvent;
import com.ecommerce.orderservice.event.OrderFailedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderStatusException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Order service — orchestrates the checkout flow:
 *
 * BEFORE (synchronous):
 *   N × product-service calls → payment-service → N × stock decrement → notification-service
 *   Latency = sum of all remote calls; threads blocked throughout.
 *
 * AFTER (optimised):
 *   1 × batch product-service call  (fixes N+1)
 *   → payment-service
 *   → N × async stock decrement     (non-blocking; fire-and-forget)
 *   → Kafka event                   (notification-service consumes asynchronously)
 *
 * API response returns as soon as the order is saved + payment decided.
 * Notifications and stock decrements happen in the background.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        log.info("Creating order for user {}", request.getUserId());

        // ── Step 1: Batch-fetch all product details in ONE round-trip ──────────
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ProductResponse> productMap = productClient.getProductsByIds(productIds);

        // ── Step 2: Validate stock and build order items ───────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            ProductResponse product = productMap.get(itemReq.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found or inactive: " + itemReq.getProductId());
            }
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is not available");
            }
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new IllegalArgumentException(
                    "Insufficient stock for '" + product.getName() + "'. Available: " + product.getStockQuantity());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .productId(product.getId()).productName(product.getName())
                    .productSku(product.getSku()).unitPrice(product.getPrice())
                    .quantity(itemReq.getQuantity()).subtotal(subtotal)
                    .build();
            orderItems.add(item);
            total = total.add(subtotal);
        }

        // ── Step 3: Persist order as PENDING ──────────────────────────────────
        Order order = Order.builder()
                .userId(request.getUserId()).shippingAddress(request.getShippingAddress())
                .totalAmount(total).status(OrderStatus.PENDING).notes(request.getNotes())
                .build();
        order = orderRepository.save(order);

        for (OrderItem item : orderItems) { item.setOrder(order); }
        order.setItems(orderItems);
        order = orderRepository.save(order);
        log.info("Order {} persisted as PENDING, total={}", order.getId(), total);

        // ── Step 4: Process payment (synchronous — we need the result) ─────────
        PaymentRequest paymentReq = PaymentRequest.builder()
                .orderId(order.getId()).userId(request.getUserId())
                .amount(total).paymentMethod(request.getPaymentMethod().name())
                .build();

        PaymentResponse paymentResp = paymentClient.chargePayment(paymentReq);

        // ── Step 5a: Payment succeeded ─────────────────────────────────────────
        if ("SUCCESS".equals(paymentResp.getStatus())) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentTransactionId(paymentResp.getTransactionId());
            log.info("Order {} CONFIRMED, txn={}", order.getId(), paymentResp.getTransactionId());

            // Decrement stock asynchronously (fire-and-forget)
            for (OrderItem item : orderItems) {
                productClient.decrementStockAsync(item.getProductId(), item.getQuantity());
            }

            // Publish event → notification-service consumes from Kafka
            kafkaTemplate.send(
                KafkaProducerConfig.TOPIC_ORDER_CONFIRMED,
                String.valueOf(order.getId()),
                OrderConfirmedEvent.builder()
                    .orderId(order.getId())
                    .userId(request.getUserId())
                    .transactionId(paymentResp.getTransactionId())
                    .totalAmount(total)
                    .build()
            );

        // ── Step 5b: Payment failed ────────────────────────────────────────────
        } else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            log.warn("Order {} PAYMENT_FAILED: {}", order.getId(), paymentResp.getMessage());

            kafkaTemplate.send(
                KafkaProducerConfig.TOPIC_ORDER_FAILED,
                String.valueOf(order.getId()),
                OrderFailedEvent.builder()
                    .orderId(order.getId())
                    .userId(request.getUserId())
                    .reason(paymentResp.getMessage())
                    .build()
            );
        }

        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        return toDto(orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional
    public OrderDto cancelOrder(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderStatusException(
                "Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order {} cancelled", id);
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        order.setStatus(newStatus);
        log.info("Order {} status updated to {}", id, newStatus);
        return toDto(orderRepository.save(order));
    }

    // -------------------------------------------------------------------------
    private OrderDto toDto(Order o) {
        List<OrderItemDto> itemDtos = o.getItems().stream().map(i ->
                OrderItemDto.builder().id(i.getId()).productId(i.getProductId())
                        .productName(i.getProductName()).productSku(i.getProductSku())
                        .unitPrice(i.getUnitPrice()).quantity(i.getQuantity())
                        .subtotal(i.getSubtotal()).build()
        ).collect(Collectors.toList());

        return OrderDto.builder().id(o.getId()).userId(o.getUserId())
                .items(itemDtos).totalAmount(o.getTotalAmount()).status(o.getStatus())
                .shippingAddress(o.getShippingAddress())
                .paymentTransactionId(o.getPaymentTransactionId())
                .notes(o.getNotes()).createdAt(o.getCreatedAt()).updatedAt(o.getUpdatedAt())
                .build();
    }
}
