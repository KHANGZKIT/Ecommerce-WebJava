package com.ecommerce.orderservice;

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
import com.ecommerce.orderservice.model.PaymentMethod;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceCurrentBehaviorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createSampleRequest(Long userId, Long productId, int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setShippingAddress("123 Test Street, Innovation Hub, Tech City, 500081");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setNotes("Leave at front door");

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(quantity);
        request.setItems(List.of(itemReq));

        return request;
    }

    private ProductResponse createSampleProduct(Long id, String name, String sku, BigDecimal price, int stock, boolean active) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .price(price)
                .stockQuantity(stock)
                .active(active)
                .build();
    }

    @Test
    void createOrder_whenPaymentSuccess_persistsConfirmedOrderAndFiresAsyncStockDecrementAndKafkaEvent() {
        CreateOrderRequest request = createSampleRequest(101L, 10L, 2);
        ProductResponse product = createSampleProduct(10L, "Wireless Mouse", "MOU-001", new BigDecimal("25.00"), 5, true);

        when(productClient.getProductsByIds(List.of(10L))).thenReturn(Map.of(10L, product));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(501L);
            }
            return order;
        });

        PaymentResponse paymentSuccess = PaymentResponse.builder()
                .status("SUCCESS")
                .transactionId("TXN-PAY-999")
                .message("Payment approved")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(paymentSuccess);

        OrderDto result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(501L);
        assertThat(result.getUserId()).isEqualTo(101L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getPaymentTransactionId()).isEqualTo("TXN-PAY-999");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("50.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSubtotal()).isEqualByComparingTo("50.00");

        verify(productClient).decrementStockAsync(10L, 2);

        ArgumentCaptor<OrderConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(kafkaTemplate).send(
                eq(KafkaProducerConfig.TOPIC_ORDER_CONFIRMED),
                eq("501"),
                eventCaptor.capture()
        );
        OrderConfirmedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(501L);
        assertThat(capturedEvent.getUserId()).isEqualTo(101L);
        assertThat(capturedEvent.getTransactionId()).isEqualTo("TXN-PAY-999");
        assertThat(capturedEvent.getTotalAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void createOrder_whenPaymentFails_persistsPaymentFailedOrderAndPublishesFailedKafkaEventWithoutStockDecrement() {
        CreateOrderRequest request = createSampleRequest(102L, 20L, 1);
        ProductResponse product = createSampleProduct(20L, "Keyboard", "KEY-001", new BigDecimal("75.00"), 10, true);

        when(productClient.getProductsByIds(List.of(20L))).thenReturn(Map.of(20L, product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(502L);
            }
            return order;
        });

        PaymentResponse paymentFailed = PaymentResponse.builder()
                .status("FAILED")
                .message("Card declined by bank")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(paymentFailed);

        OrderDto result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(502L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(result.getPaymentTransactionId()).isNull();

        verify(productClient, never()).decrementStockAsync(anyLong(), anyInt());

        ArgumentCaptor<OrderFailedEvent> eventCaptor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(kafkaTemplate).send(
                eq(KafkaProducerConfig.TOPIC_ORDER_FAILED),
                eq("502"),
                eventCaptor.capture()
        );
        OrderFailedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(502L);
        assertThat(capturedEvent.getUserId()).isEqualTo(102L);
        assertThat(capturedEvent.getReason()).isEqualTo("Card declined by bank");
    }

    @Test
    void createOrder_calculatesOrderTotalFromProductPrices() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(200L);
        request.setShippingAddress("456 Market Road, Financial District, Tech City, 500081");
        request.setPaymentMethod(PaymentMethod.UPI);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(3);

        request.setItems(List.of(item1, item2));

        ProductResponse prod1 = createSampleProduct(1L, "Item 1", "SKU-1", new BigDecimal("12.50"), 10, true);
        ProductResponse prod2 = createSampleProduct(2L, "Item 2", "SKU-2", new BigDecimal("20.00"), 10, true);

        when(productClient.getProductsByIds(List.of(1L, 2L))).thenReturn(Map.of(1L, prod1, 2L, prod2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(600L);
            }
            return order;
        });
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(
                PaymentResponse.builder().status("SUCCESS").transactionId("TXN-1").build()
        );

        OrderDto result = orderService.createOrder(request);

        // Expected total: (2 * 12.50) + (3 * 20.00) = 25.00 + 60.00 = 85.00
        assertThat(result.getTotalAmount()).isEqualByComparingTo("85.00");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(result.getItems().get(0).getSubtotal()).isEqualByComparingTo("25.00");
        assertThat(result.getItems().get(1).getUnitPrice()).isEqualByComparingTo("20.00");
        assertThat(result.getItems().get(1).getSubtotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void createOrder_whenProductNotFound_throwsIllegalArgumentException() {
        CreateOrderRequest request = createSampleRequest(101L, 999L, 1);
        when(productClient.getProductsByIds(List.of(999L))).thenReturn(Map.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found or inactive: 999");

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentClient);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void createOrder_whenProductInactive_throwsIllegalArgumentException() {
        CreateOrderRequest request = createSampleRequest(101L, 30L, 1);
        ProductResponse inactiveProduct = createSampleProduct(30L, "Discontinued Item", "DISC-01", new BigDecimal("10.00"), 5, false);

        when(productClient.getProductsByIds(List.of(30L))).thenReturn(Map.of(30L, inactiveProduct));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product 'Discontinued Item' is not available");

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentClient);
    }

    @Test
    void createOrder_whenInsufficientStock_throwsIllegalArgumentException() {
        CreateOrderRequest request = createSampleRequest(101L, 40L, 10);
        ProductResponse lowStockProduct = createSampleProduct(40L, "Limited Item", "LIM-01", new BigDecimal("15.00"), 3, true);

        when(productClient.getProductsByIds(List.of(40L))).thenReturn(Map.of(40L, lowStockProduct));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock for 'Limited Item'. Available: 3");

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(paymentClient);
    }

    @Test
    void createOrder_whenSameProductMultipleTimes_validatesStockPerItemIndividually() {
        // Documenting current behavior: stock is checked against item quantity individually, not accumulated
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(101L);
        request.setShippingAddress("123 Test Street, Innovation Hub, Tech City, 500081");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(50L);
        item1.setQuantity(4);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(50L);
        item2.setQuantity(4);

        request.setItems(List.of(item1, item2));

        // Available stock is 5 (less than 4+4=8, but each item has quantity 4 <= 5)
        ProductResponse product = createSampleProduct(50L, "Duplicate Product", "DUP-01", new BigDecimal("10.00"), 5, true);
        when(productClient.getProductsByIds(List.of(50L))).thenReturn(Map.of(50L, product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(700L);
            }
            return order;
        });
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(
                PaymentResponse.builder().status("SUCCESS").transactionId("TXN-DUP").build()
        );

        OrderDto result = orderService.createOrder(request);

        // Characterizes that both items passed validation because 4 <= 5 on each iteration
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void createOrder_whenPaymentClientThrowsException_propagatesException() {
        CreateOrderRequest request = createSampleRequest(101L, 10L, 1);
        ProductResponse product = createSampleProduct(10L, "Mouse", "MOU", new BigDecimal("20.00"), 5, true);

        when(productClient.getProductsByIds(List.of(10L))).thenReturn(Map.of(10L, product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(800L);
            return o;
        });
        when(paymentClient.chargePayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Payment service connection timed out"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment service connection timed out");

        verify(productClient, never()).decrementStockAsync(anyLong(), anyInt());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void cancelOrder_whenPendingOrConfirmed_transitionsToCancelledWithoutRestockOrRefund() {
        Order pendingOrder = Order.builder()
                .id(901L)
                .userId(101L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .items(new ArrayList<>())
                .build();

        Order confirmedOrder = Order.builder()
                .id(902L)
                .userId(101L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("50.00"))
                .items(new ArrayList<>())
                .paymentTransactionId("TXN-902")
                .build();

        when(orderRepository.findByIdWithItems(901L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.findByIdWithItems(902L)).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto cancelledPending = orderService.cancelOrder(901L);
        assertThat(cancelledPending.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OrderDto cancelledConfirmed = orderService.cancelOrder(902L);
        assertThat(cancelledConfirmed.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Documenting current behavior: cancelOrder does not call refund or restock
        verifyNoInteractions(paymentClient);
        verify(productClient, never()).decrementStockAsync(anyLong(), anyInt());
    }

    @Test
    void cancelOrder_whenAlreadyCancelledOrFailed_throwsOrderStatusException() {
        Order cancelledOrder = Order.builder().id(903L).status(OrderStatus.CANCELLED).items(List.of()).build();
        Order failedOrder = Order.builder().id(904L).status(OrderStatus.PAYMENT_FAILED).items(List.of()).build();
        Order shippedOrder = Order.builder().id(905L).status(OrderStatus.SHIPPED).items(List.of()).build();

        when(orderRepository.findByIdWithItems(903L)).thenReturn(Optional.of(cancelledOrder));
        when(orderRepository.findByIdWithItems(904L)).thenReturn(Optional.of(failedOrder));
        when(orderRepository.findByIdWithItems(905L)).thenReturn(Optional.of(shippedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(903L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("Cannot cancel order in status: CANCELLED");

        assertThatThrownBy(() -> orderService.cancelOrder(904L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("Cannot cancel order in status: PAYMENT_FAILED");

        assertThatThrownBy(() -> orderService.cancelOrder(905L))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("Cannot cancel order in status: SHIPPED");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_allowsArbitraryTransitionsWithoutValidation() {
        Order order = Order.builder()
                .id(906L)
                .status(OrderStatus.CANCELLED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithItems(906L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Documents current behavior: CANCELLED -> DELIVERED transition is accepted
        OrderDto updated = orderService.updateOrderStatus(906L, OrderStatus.DELIVERED);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void getOrderById_whenNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found: 999");
    }

    @Test
    void getOrderById_whenFound_returnsOrderDto() {
        Order order = Order.builder()
                .id(1001L)
                .userId(50L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("120.00"))
                .items(List.of(OrderItem.builder()
                        .id(1L)
                        .productId(10L)
                        .productName("Item")
                        .productSku("SKU")
                        .unitPrice(new BigDecimal("60.00"))
                        .quantity(2)
                        .subtotal(new BigDecimal("120.00"))
                        .build()))
                .build();

        when(orderRepository.findByIdWithItems(1001L)).thenReturn(Optional.of(order));

        OrderDto dto = orderService.getOrderById(1001L);
        assertThat(dto.getId()).isEqualTo(1001L);
        assertThat(dto.getUserId()).isEqualTo(50L);
        assertThat(dto.getItems()).hasSize(1);
    }

    @Test
    void getOrdersByUserId_returnsPagedOrdersForUserId() {
        Order order = Order.builder()
                .id(2001L)
                .userId(77L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("45.00"))
                .items(new ArrayList<>())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByUserId(77L, pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderDto> pageResult = orderService.getOrdersByUserId(77L, pageable);
        assertThat(pageResult.getTotalElements()).isEqualTo(1);
        assertThat(pageResult.getContent().get(0).getId()).isEqualTo(2001L);
        assertThat(pageResult.getContent().get(0).getUserId()).isEqualTo(77L);
    }
}
