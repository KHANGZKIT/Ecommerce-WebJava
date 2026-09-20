package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.CartClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.controller.OrderController;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.model.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.security.OrderPrincipal;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationPhase4Test {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderIdempotencyService orderIdempotencyService;

    private OrderService orderService;
    private OrderController orderController;

    private CheckoutRequest sampleRequest;
    private final Long userId = 42L;
    private final String idempotencyKey = "test-key-phase4";
    private final String bearerToken = "valid-customer-jwt-token";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                kafkaTemplate,
                cartClient,
                orderIdempotencyService
        );
        orderController = new OrderController(orderService, orderIdempotencyService);

        sampleRequest = CheckoutRequest.builder()
                .shippingAddress("123 Tech Avenue, Silicon Hills, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Phase 4 cart clear checkout")
                .build();

        lenient().when(orderIdempotencyService.claimPaymentAttempt(any())).thenReturn(true);
    }

    private OrderIdempotency mockNewIdempotencyRecord() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(1001L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint("mock-fingerprint-phase4")
                .cartClearStatus(CartClearStatus.NOT_REQUIRED)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);
        lenient().when(orderIdempotencyService.find(eq(userId), eq(idempotencyKey)))
                .thenReturn(Optional.of(record));
        return record;
    }

    private void mockCartAndProductHold(BigDecimal price, int quantity) {
        CartItemResponse item = new CartItemResponse(10L, quantity);
        when(cartClient.getCart(bearerToken)).thenReturn(CartResponse.builder().items(List.of(item)).build());

        HeldItemDto heldItem = new HeldItemDto(10L, "Mechanical Keyboard", "KB-01", price, quantity);
        when(productClient.holdStock(any(HoldStockRequest.class))).thenReturn(new HoldStockResponse(List.of(heldItem)));
    }

    private void mockOrderPersistence(OrderIdempotency record, BigDecimal total) {
        Order savedOrder = Order.builder()
                .id(501L)
                .userId(userId)
                .shippingAddress(sampleRequest.getShippingAddress())
                .notes(sampleRequest.getNotes())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();

        when(orderIdempotencyService.persistPendingOrderAndLink(any(Order.class), anyList(), eq(record.getId()), eq(idempotencyKey)))
                .thenReturn(savedOrder);
    }

    private PaymentResponse mockPaymentSuccess() {
        PaymentResponse successResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-phase4-success")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(successResponse);
        return successResponse;
    }

    // -------------------------------------------------------------------------
    // 1. paymentSuccess_clearsCartAndMarksCartClearStatusCleared
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. Successful payment clears Cart and marks cartClearStatus as CLEARED")
    void paymentSuccess_clearsCartAndMarksCartClearStatusCleared() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));
        mockPaymentSuccess();

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartClient).clearCart(bearerToken);
        verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), eq(idempotencyKey), any(), any(), eq(CartClearStatus.CLEARED));
        verify(productClient, never()).releaseStock(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 2. paymentSuccess_clearsCartUsingVerifiedBearerToken
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Cart clear uses the verified customer Bearer token without passing customer/item parameters")
    void paymentSuccess_clearsCartUsingVerifiedBearerToken() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));
        mockPaymentSuccess();

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(cartClient).clearCart(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isEqualTo(bearerToken);
    }

    // -------------------------------------------------------------------------
    // 3. cartClearFailure_keepsOrderConfirmedAndMarksClearPending
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Cart clear failure keeps Order as CONFIRMED and marks cartClearStatus as PENDING")
    void cartClearFailure_keepsOrderConfirmedAndMarksClearPending() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));
        mockPaymentSuccess();

        doThrow(new ApiException("Cart service unavailable", HttpStatus.SERVICE_UNAVAILABLE))
                .when(cartClient).clearCart(any());

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), eq(idempotencyKey), any(), any(), eq(CartClearStatus.PENDING));
        verify(productClient, never()).releaseStock(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 4. cartClearFailure_returnsAcceptedSemantics
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Cart clear failure returns 202 Accepted semantics for CONFIRMED_CART_CLEAR_PENDING")
    void cartClearFailure_returnsAcceptedSemantics() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));
        mockPaymentSuccess();

        doAnswer(inv -> {
            record.setCartClearStatus(CartClearStatus.PENDING);
            return record;
        }).when(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), eq(idempotencyKey), any(), any(), eq(CartClearStatus.PENDING));

        doThrow(new ApiException("Cart service unavailable", HttpStatus.SERVICE_UNAVAILABLE))
                .when(cartClient).clearCart(any());

        ResponseEntity<Map<String, Object>> response = orderController.checkout(
                idempotencyKey, bearerToken, sampleRequest, new OrderPrincipal(userId, "customer@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "CONFIRMED_CART_CLEAR_PENDING");
        assertThat(response.getBody()).containsEntry("orderId", 501L);
    }

    // -------------------------------------------------------------------------
    // 5. confirmedClearPendingReplay_onlyRetriesCartClear
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Replaying a CONFIRMED order with cartClearStatus PENDING only retries Cart clear")
    void confirmedClearPendingReplay_onlyRetriesCartClear() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(888L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartClient, times(1)).clearCart(bearerToken);
    }

    // -------------------------------------------------------------------------
    // 6. confirmedClearPendingReplay_doesNotChargePaymentAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Replaying a CONFIRMED order with cartClearStatus PENDING does NOT charge Payment again")
    void confirmedClearPendingReplay_doesNotChargePaymentAgain() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(paymentClient, never()).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 7. confirmedClearPendingReplay_doesNotHoldProductAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Replaying a CONFIRMED order with cartClearStatus PENDING does NOT hold Product stock again")
    void confirmedClearPendingReplay_doesNotHoldProductAgain() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(productClient, never()).holdStock(any());
        verify(productClient, never()).releaseStock(any());
    }

    // -------------------------------------------------------------------------
    // 8. confirmedClearPendingReplay_doesNotFetchCartAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Replaying a CONFIRMED order with cartClearStatus PENDING does NOT fetch Cart again")
    void confirmedClearPendingReplay_doesNotFetchCartAgain() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(cartClient, never()).getCart(any());
    }

    // -------------------------------------------------------------------------
    // 9. successfulCartClearAfterRetry_marksStatusCleared
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Successful Cart clear on retry updates cartClearStatus to CLEARED")
    void successfulCartClearAfterRetry_marksStatusCleared() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        doAnswer(inv -> {
            record.setCartClearStatus(CartClearStatus.CLEARED);
            return record;
        }).when(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), eq(idempotencyKey), any(), any(), eq(CartClearStatus.CLEARED));

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(888L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartClient, times(1)).clearCart(bearerToken);
        verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), eq(idempotencyKey), any(), any(), eq(CartClearStatus.CLEARED));
    }

    // -------------------------------------------------------------------------
    // 10. failedCartClearRetry_keepsStatusPending
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Failed Cart clear on retry keeps cartClearStatus as PENDING and returns 202 Accepted")
    void failedCartClearRetry_keepsStatusPending() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.PENDING)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);
        lenient().when(orderIdempotencyService.find(eq(userId), eq(idempotencyKey)))
                .thenReturn(Optional.of(record));

        doThrow(new ApiException("Cart service unavailable", HttpStatus.SERVICE_UNAVAILABLE))
                .when(cartClient).clearCart(any());

        ResponseEntity<Map<String, Object>> response = orderController.checkout(
                idempotencyKey, bearerToken, sampleRequest, new OrderPrincipal(userId, "customer@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "CONFIRMED_CART_CLEAR_PENDING");
        verify(paymentClient, never()).chargePayment(any());
        verify(productClient, never()).holdStock(any());
    }

    // -------------------------------------------------------------------------
    // 11. confirmedAndClearedReplay_hasNoSideEffects
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. Replaying a CONFIRMED order with cartClearStatus CLEARED returns immediately with no side effects")
    void confirmedAndClearedReplay_hasNoSideEffects() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .cartClearStatus(CartClearStatus.CLEARED)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(888L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartClient, never()).clearCart(any());
        verify(cartClient, never()).getCart(any());
        verify(paymentClient, never()).chargePayment(any());
        verify(productClient, never()).holdStock(any());
        verify(productClient, never()).releaseStock(any());
    }
}
