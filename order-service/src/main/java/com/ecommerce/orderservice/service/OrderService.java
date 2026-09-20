package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.config.KafkaProducerConfig;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.event.OrderConfirmedEvent;
import com.ecommerce.orderservice.event.OrderFailedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderStatusException;
import com.ecommerce.orderservice.model.CartClearStatus;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.model.StockReleaseStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.ecommerce.orderservice.client.CartClient;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CartClient cartClient;
    private final OrderIdempotencyService orderIdempotencyService;

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        PaymentClient paymentClient,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this(orderRepository, productClient, paymentClient, kafkaTemplate, null, null);
    }

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        PaymentClient paymentClient,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        @Autowired(required = false) CartClient cartClient,
                        @Autowired(required = false) OrderIdempotencyService orderIdempotencyService) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.kafkaTemplate = kafkaTemplate;
        this.cartClient = cartClient;
        this.orderIdempotencyService = orderIdempotencyService;
    }

    public Order checkout(Long userId, String idempotencyKey, CheckoutRequest request, String bearerToken) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Checkout request cannot be null");
        }
        orderIdempotencyService.validateKey(idempotencyKey);

        OrderIdempotency idempotencyRecord = orderIdempotencyService.getOrCreate(userId, idempotencyKey, request);
        if (idempotencyRecord.getOrder() != null) {
            Order existingOrder = idempotencyRecord.getOrder();
            OrderStatus status = existingOrder.getStatus();
            if (status == OrderStatus.CONFIRMED) {
                if (idempotencyRecord.getCartClearStatus() == CartClearStatus.PENDING) {
                    log.info("Order {} is CONFIRMED with cartClearStatus PENDING. Retrying Cart clear for key {}",
                            existingOrder.getId(), idempotencyKey);
                    clearCart(bearerToken, idempotencyRecord, idempotencyKey);
                } else {
                    log.info("Returning existing order {} in status CONFIRMED for replayed idempotency key {}",
                            existingOrder.getId(), idempotencyKey);
                }
                return existingOrder;
            }
            if (status == OrderStatus.PAYMENT_FAILED || status == OrderStatus.PAYMENT_UNKNOWN) {
                log.info("Returning existing order {} in status {} for replayed idempotency key {}",
                        existingOrder.getId(), status, idempotencyKey);
                return existingOrder;
            }
            if (status == OrderStatus.PENDING) {
                if (idempotencyRecord.getPaymentAttemptStartedAt() != null) {
                    log.info("Returning existing order {} in status PENDING (payment already dispatched at {}) for replayed idempotency key {}",
                            existingOrder.getId(), idempotencyRecord.getPaymentAttemptStartedAt(), idempotencyKey);
                    return existingOrder;
                }
                return processPayment(existingOrder, idempotencyRecord, idempotencyKey, request, bearerToken);
            }
            return existingOrder;
        }

        CartResponse cart = cartClient.getCart(bearerToken);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<StockItemRequest> stockItems = cart.getItems().stream()
                .map(i -> new StockItemRequest(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        HoldStockRequest holdRequest = HoldStockRequest.builder()
                .checkoutOperationKey(idempotencyKey)
                .items(stockItems)
                .build();

        HoldStockResponse holdResponse = productClient.holdStock(holdRequest);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (HeldItemDto heldItem : holdResponse.getItems()) {
            BigDecimal subtotal = heldItem.getPrice().multiply(BigDecimal.valueOf(heldItem.getQuantity()));
            total = total.add(subtotal);
            OrderItem item = OrderItem.builder()
                    .productId(heldItem.getProductId())
                    .productName(heldItem.getName())
                    .productSku(heldItem.getSku())
                    .unitPrice(heldItem.getPrice())
                    .quantity(heldItem.getQuantity())
                    .subtotal(subtotal)
                    .build();
            orderItems.add(item);
        }

        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .notes(request.getNotes())
                .build();

        try {
            order = orderIdempotencyService.persistPendingOrderAndLink(
                    order, orderItems, idempotencyRecord.getId(), idempotencyKey);
        } catch (Exception ex) {
            log.error("Order persistence failed after product stock hold for operationKey {}: {}",
                    idempotencyKey, ex.getMessage());
            try {
                productClient.releaseStock(new ReleaseStockRequest(idempotencyKey));
            } catch (Exception releaseEx) {
                log.error("Failed to release stock for operationKey {} after order persistence error: {}",
                        idempotencyKey, releaseEx.getMessage());
            }
            throw ex;
        }

        return processPayment(order, idempotencyRecord, idempotencyKey, request, bearerToken);
    }

    private Order processPayment(Order order, OrderIdempotency idempotencyRecord, String idempotencyKey, CheckoutRequest request, String bearerToken) {
        if (orderIdempotencyService != null && idempotencyRecord != null && idempotencyRecord.getId() != null) {
            boolean claimGranted = orderIdempotencyService.claimPaymentAttempt(idempotencyRecord.getId());
            if (!claimGranted) {
                log.info("Payment attempt claim denied for order {}. Returning existing order without charging.", order.getId());
                if (orderRepository != null && order.getId() != null) {
                    return orderRepository.findById(order.getId()).orElse(order);
                }
                return order;
            }
            idempotencyRecord.setPaymentAttemptStartedAt(LocalDateTime.now());
        }

        if (paymentClient == null) {
            return order;
        }

        PaymentRequest paymentReq = PaymentRequest.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod().name() : null)
                .build();

        PaymentResponse paymentResp;
        try {
            paymentResp = paymentClient.chargePayment(paymentReq);
        } catch (Exception ex) {
            log.error("Payment charge failed with unexpected exception for order {}: {}", order.getId(), ex.getMessage(), ex);
            paymentResp = PaymentResponse.builder()
                    .orderId(order.getId())
                    .status(PaymentResponse.STATUS_UNKNOWN)
                    .message("Unexpected payment exception: " + ex.getMessage())
                    .build();
        }

        if (paymentResp == null || paymentResp.getStatus() == null) {
            order.setStatus(OrderStatus.PAYMENT_UNKNOWN);
            if (orderRepository != null) {
                Order saved = orderRepository.save(order);
                if (saved != null) {
                    order = saved;
                }
            }
            log.warn("Order {} in PAYMENT_UNKNOWN status due to null or invalid payment response", order.getId());
            return order;
        }

        boolean isValidSuccess = PaymentResponse.STATUS_SUCCESS.equals(paymentResp.getStatus())
                && paymentResp.getOrderId() != null
                && paymentResp.getOrderId().equals(order.getId())
                && paymentResp.getTransactionId() != null
                && !paymentResp.getTransactionId().isBlank();

        if (isValidSuccess) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentTransactionId(paymentResp.getTransactionId());
            if (orderRepository != null) {
                Order saved = orderRepository.save(order);
                if (saved != null) {
                    order = saved;
                }
            }
            log.info("Order {} CONFIRMED with txn {}", order.getId(), paymentResp.getTransactionId());
            clearCart(bearerToken, idempotencyRecord, idempotencyKey);
            return order;
        } else if (PaymentResponse.STATUS_FAILED.equals(paymentResp.getStatus())) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            if (orderRepository != null) {
                Order saved = orderRepository.save(order);
                if (saved != null) {
                    order = saved;
                }
            }
            log.warn("Order {} PAYMENT_FAILED: {}", order.getId(), paymentResp.getMessage());

            if (orderIdempotencyService != null && idempotencyRecord != null && idempotencyRecord.getId() != null) {
                idempotencyRecord.setStockReleaseStatus(StockReleaseStatus.PENDING);
                orderIdempotencyService.updateRecoveryMetadata(
                        idempotencyRecord.getId(),
                        idempotencyKey,
                        null,
                        StockReleaseStatus.PENDING,
                        null
                );
            }

            try {
                if (productClient != null) {
                    productClient.releaseStock(new ReleaseStockRequest(idempotencyKey));
                }
                if (orderIdempotencyService != null && idempotencyRecord != null && idempotencyRecord.getId() != null) {
                    idempotencyRecord.setStockReleaseStatus(StockReleaseStatus.RELEASED);
                    orderIdempotencyService.updateRecoveryMetadata(
                            idempotencyRecord.getId(),
                            idempotencyKey,
                            null,
                            StockReleaseStatus.RELEASED,
                            null
                    );
                }
            } catch (Exception ex) {
                log.error("Stock release compensation failed for operationKey {} after payment failure: {}",
                        idempotencyKey, ex.getMessage(), ex);
            }

            return order;
        } else {
            order.setStatus(OrderStatus.PAYMENT_UNKNOWN);
            if (orderRepository != null) {
                Order saved = orderRepository.save(order);
                if (saved != null) {
                    order = saved;
                }
            }
            log.warn("Order {} in PAYMENT_UNKNOWN status: status={}, message={}",
                    order.getId(), paymentResp.getStatus(), paymentResp.getMessage());
            return order;
        }
    }

    private void clearCart(String bearerToken, OrderIdempotency idempotencyRecord, String idempotencyKey) {
        if (cartClient == null) {
            return;
        }
        try {
            cartClient.clearCart(bearerToken);
            if (orderIdempotencyService != null && idempotencyRecord != null && idempotencyRecord.getId() != null) {
                idempotencyRecord.setCartClearStatus(CartClearStatus.CLEARED);
                orderIdempotencyService.updateRecoveryMetadata(
                        idempotencyRecord.getId(),
                        idempotencyKey,
                        null,
                        null,
                        CartClearStatus.CLEARED
                );
            }
            log.info("Cart cleared successfully for idempotencyKey {}", idempotencyKey);
        } catch (Exception ex) {
            log.warn("Cart clear failed for idempotencyKey {}: {}", idempotencyKey, ex.getMessage());
            if (orderIdempotencyService != null && idempotencyRecord != null && idempotencyRecord.getId() != null) {
                idempotencyRecord.setCartClearStatus(CartClearStatus.PENDING);
                orderIdempotencyService.updateRecoveryMetadata(
                        idempotencyRecord.getId(),
                        idempotencyKey,
                        null,
                        null,
                        CartClearStatus.PENDING
                );
            }
        }
    }

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
