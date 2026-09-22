package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.model.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CartService;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationPhase3Test {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private PaymentClient paymentClient;


    @Mock
    private CartService cartService;

    @Mock
    private OrderIdempotencyService orderIdempotencyService;

    private OrderService orderService;

    private CheckoutRequest sampleRequest;
    private final Long userId = 42L;
    private final String idempotencyKey = "test-key-phase3";
    private final String bearerToken = "valid-jwt-token";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                cartService,
                orderIdempotencyService
        );

        sampleRequest = CheckoutRequest.builder()
                .shippingAddress("123 Tech Avenue, Silicon Hills, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Phase 3 payment checkout")
                .build();

        lenient().when(orderIdempotencyService.claimPaymentAttempt(any())).thenReturn(true);
    }

    private OrderIdempotency mockNewIdempotencyRecord() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(1001L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint("mock-fingerprint-phase3")
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);
        return record;
    }

    private void mockCartAndProductHold(BigDecimal price, int quantity) {
        CartItemDto item = CartItemDto.builder().productId(10L).quantity(quantity).build();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(item)).build());

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

    // -------------------------------------------------------------------------
    // 1. checkout_whenPaymentSucceeds_marksOrderConfirmed
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. Successful payment charges Order and updates status to CONFIRMED with transactionId")
    void checkout_whenPaymentSucceeds_marksOrderConfirmed() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse successResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-success-999")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(successResponse);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getPaymentTransactionId()).isEqualTo("txn-success-999");
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED && "txn-success-999".equals(o.getPaymentTransactionId())));
        verify(productClient, never()).releaseStock(any());
        verify(cartService).clearCart(userId);
    }

    // -------------------------------------------------------------------------
    // 2. paymentAmountComesFromPersistedOrderTotal
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Payment charge amount must come strictly from the persisted Order totalAmount")
    void paymentAmountComesFromPersistedOrderTotal() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        // 2 items @ 124.50 = 249.00
        BigDecimal expectedTotal = new BigDecimal("249.00");
        mockCartAndProductHold(new BigDecimal("124.50"), 2);
        mockOrderPersistence(record, expectedTotal);

        PaymentResponse successResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-249")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(successResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentClient).chargePayment(captor.capture());

        PaymentRequest paymentReq = captor.getValue();
        assertThat(paymentReq.getOrderId()).isEqualTo(501L);
        assertThat(paymentReq.getUserId()).isEqualTo(userId);
        assertThat(paymentReq.getAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(paymentReq.getPaymentMethod()).isEqualTo("CREDIT_CARD");
    }

    // -------------------------------------------------------------------------
    // 3. checkout_whenPaymentFails_marksOrderPaymentFailed
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Definite payment failure marks order as PAYMENT_FAILED")
    void checkout_whenPaymentFails_marksOrderPaymentFailed() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("50.00"), 1);
        mockOrderPersistence(record, new BigDecimal("50.00"));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Card declined by issuer")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedResponse);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_FAILED));
    }

    // -------------------------------------------------------------------------
    // 4. paymentFailure_releasesHeldStock
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Payment failure triggers stock release with stockReleaseStatus PENDING -> RELEASED")
    void paymentFailure_releasesHeldStock() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("50.00"), 1);
        mockOrderPersistence(record, new BigDecimal("50.00"));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Insufficient funds")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        InOrder inOrder = inOrder(orderIdempotencyService, productClient);
        // First, stockReleaseStatus marked PENDING before release call
        inOrder.verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), any(), any(), eq(StockReleaseStatus.PENDING), any());

        // Then releaseStock called with operation key = idempotencyKey
        ArgumentCaptor<ReleaseStockRequest> releaseCaptor = ArgumentCaptor.forClass(ReleaseStockRequest.class);
        inOrder.verify(productClient).releaseStock(releaseCaptor.capture());
        assertThat(releaseCaptor.getValue().getCheckoutOperationKey()).isEqualTo(idempotencyKey);

        // Finally, stockReleaseStatus marked RELEASED
        inOrder.verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), any(), any(), eq(StockReleaseStatus.RELEASED), any());
    }

    // -------------------------------------------------------------------------
    // 5. checkout_whenPaymentIsUnknown_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Ambiguous transport failure / 5xx marks order as PAYMENT_UNKNOWN")
    void checkout_whenPaymentIsUnknown_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("75.00"), 1);
        mockOrderPersistence(record, new BigDecimal("75.00"));

        PaymentResponse unknownResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_UNKNOWN)
                .message("Payment service 5xx server error")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(unknownResponse);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
    }

    // -------------------------------------------------------------------------
    // 6. paymentUnknown_doesNotReleaseStock
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Ambiguous payment UNKNOWN must NOT release stock (stock remains held)")
    void paymentUnknown_doesNotReleaseStock() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("75.00"), 1);
        mockOrderPersistence(record, new BigDecimal("75.00"));

        PaymentResponse unknownResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_UNKNOWN)
                .message("Socket timeout calling payment-service")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(unknownResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(productClient, never()).releaseStock(any());
        verify(orderIdempotencyService, never()).updateRecoveryMetadata(any(), any(), any(), eq(StockReleaseStatus.RELEASED), any());
    }

    // -------------------------------------------------------------------------
    // 7. paymentUnknown_doesNotRetryCharge
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Ambiguous payment UNKNOWN does not retry charge; exactly one charge attempt is made")
    void paymentUnknown_doesNotRetryCharge() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("75.00"), 1);
        mockOrderPersistence(record, new BigDecimal("75.00"));

        PaymentResponse unknownResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_UNKNOWN)
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(unknownResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(paymentClient, times(1)).chargePayment(any(PaymentRequest.class));
    }

    // -------------------------------------------------------------------------
    // 8. confirmedOrderReplay_doesNotChargeAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Replaying a CONFIRMED order returns it immediately without calling payment")
    void confirmedOrderReplay_doesNotChargeAgain() {
        Order confirmedOrder = Order.builder()
                .id(888L)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("existing-txn-888")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1002L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(confirmedOrder)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(888L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(paymentClient, never()).chargePayment(any());
        verify(cartService, never()).getCartByUserId(any());
        verify(productClient, never()).holdStock(any());
    }

    // -------------------------------------------------------------------------
    // 9. failedOrderReplay_doesNotChargeAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Replaying a PAYMENT_FAILED order returns it immediately without charging or holding stock again")
    void failedOrderReplay_doesNotChargeAgain() {
        Order failedOrder = Order.builder()
                .id(889L)
                .userId(userId)
                .status(OrderStatus.PAYMENT_FAILED)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1003L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(failedOrder)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(889L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(paymentClient, never()).chargePayment(any());
        verify(cartService, never()).getCartByUserId(any());
        verify(productClient, never()).holdStock(any());
    }

    // -------------------------------------------------------------------------
    // 10. unknownOrderReplay_doesNotChargeAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Replaying a PAYMENT_UNKNOWN order returns it immediately without charging again or releasing stock")
    void unknownOrderReplay_doesNotChargeAgain() {
        Order unknownOrder = Order.builder()
                .id(890L)
                .userId(userId)
                .status(OrderStatus.PAYMENT_UNKNOWN)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1004L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(unknownOrder)
                .paymentAttemptStartedAt(LocalDateTime.now().minusMinutes(2))
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(890L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(paymentClient, never()).chargePayment(any());
        verify(productClient, never()).releaseStock(any());
    }

    // -------------------------------------------------------------------------
    // 11. pendingOrderWithPaymentAttemptStarted_doesNotChargeAgain
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. Replaying a PENDING order with non-null paymentAttemptStartedAt does NOT charge again")
    void pendingOrderWithPaymentAttemptStarted_doesNotChargeAgain() {
        Order pendingOrder = Order.builder()
                .id(891L)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1005L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(pendingOrder)
                .paymentAttemptStartedAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(891L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentClient, never()).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 12. paymentReleaseFailure_doesNotHidePaymentFailedState
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("12. If Product RELEASE fails after payment failure, Order remains PAYMENT_FAILED and stockReleaseStatus remains PENDING")
    void paymentReleaseFailure_doesNotHidePaymentFailedState() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("50.00"), 1);
        mockOrderPersistence(record, new BigDecimal("50.00"));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Insufficient funds")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedResponse);

        // Product release fails with 503
        when(productClient.releaseStock(any(ReleaseStockRequest.class)))
                .thenThrow(new ApiException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_FAILED));
        // stockReleaseStatus is marked PENDING before release attempt
        verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()), any(), any(), eq(StockReleaseStatus.PENDING), any());
        // stockReleaseStatus is NEVER marked RELEASED because release threw exception
        verify(orderIdempotencyService, never()).updateRecoveryMetadata(
                eq(record.getId()), any(), any(), eq(StockReleaseStatus.RELEASED), any());
    }

    // -------------------------------------------------------------------------
    // 13. paymentAttemptRecordedBeforePaymentCharge
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13. paymentAttemptStartedAt is persisted before chargePayment is dispatched")
    void paymentAttemptRecordedBeforePaymentCharge() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse successResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-order-13")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(successResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        InOrder inOrder = inOrder(orderIdempotencyService, paymentClient);
        inOrder.verify(orderIdempotencyService).claimPaymentAttempt(eq(record.getId()));
        inOrder.verify(paymentClient).chargePayment(any(PaymentRequest.class));
    }

    // -------------------------------------------------------------------------
    // 14. pendingOrderWithoutPaymentAttempt_resumesPaymentAttempt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("14. Replaying a PENDING order with null paymentAttemptStartedAt resumes single payment attempt")
    void pendingOrderWithoutPaymentAttempt_resumesPaymentAttempt() {
        Order pendingOrder = Order.builder()
                .id(892L)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("150.00"))
                .build();

        OrderIdempotency record = OrderIdempotency.builder()
                .id(1006L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(pendingOrder)
                .paymentAttemptStartedAt(null)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);

        PaymentResponse successResponse = PaymentResponse.builder()
                .orderId(892L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-resumed-892")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(successResponse);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getId()).isEqualTo(892L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getPaymentTransactionId()).isEqualTo("txn-resumed-892");

        // Cart and Product hold should NEVER be called on resume
        verify(cartService, never()).getCartByUserId(any());
        verify(productClient, never()).holdStock(any());

        // Payment charge MUST be called once with order total
        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentClient, times(1)).chargePayment(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(892L);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    // -------------------------------------------------------------------------
    // 15. paymentFailure_setsStockReleasePendingBeforeCompensation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("15. After Payment returns FAILED, stockReleaseStatus is set to PENDING before Product RELEASE is attempted")
    void paymentFailure_setsStockReleasePendingBeforeCompensation() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("60.00"), 1);
        mockOrderPersistence(record, new BigDecimal("60.00"));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Card declined")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        InOrder inOrder = inOrder(orderIdempotencyService, productClient);
        inOrder.verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()),
                any(),
                any(),
                eq(StockReleaseStatus.PENDING),
                any()
        );
        inOrder.verify(productClient).releaseStock(any(ReleaseStockRequest.class));
    }

    // -------------------------------------------------------------------------
    // 16. successfulStockRelease_marksReleaseStatusReleased
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("16. When Product RELEASE succeeds, stockReleaseStatus becomes RELEASED")
    void successfulStockRelease_marksReleaseStatusReleased() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("70.00"), 1);
        mockOrderPersistence(record, new BigDecimal("70.00"));

        PaymentResponse failedResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Card declined")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedResponse);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        InOrder inOrder = inOrder(productClient, orderIdempotencyService);
        inOrder.verify(productClient).releaseStock(argThat(req -> idempotencyKey.equals(req.getCheckoutOperationKey())));
        inOrder.verify(orderIdempotencyService).updateRecoveryMetadata(
                eq(record.getId()),
                any(),
                any(),
                eq(StockReleaseStatus.RELEASED),
                any()
        );
    }

    // -------------------------------------------------------------------------
    // 17. nullPaymentResponse_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("17. Null PaymentResponse marks order as PAYMENT_UNKNOWN, keeps stock held, and does not retry")
    void nullPaymentResponse_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(null);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 18. nullPaymentStatus_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("18. Null or invalid Payment status marks order as PAYMENT_UNKNOWN, keeps stock held, and does not retry")
    void nullPaymentStatus_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse nullStatusResponse = PaymentResponse.builder()
                .orderId(501L)
                .status(null)
                .message("Malformed response without status")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(nullStatusResponse);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 19. paymentAttemptClaim_whenUnclaimed_allowsExactlyOnePaymentAttempt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("19. When payment attempt is unclaimed, claim is granted and allows exactly one payment attempt")
    void paymentAttemptClaim_whenUnclaimed_allowsExactlyOnePaymentAttempt() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        when(orderIdempotencyService.claimPaymentAttempt(record.getId())).thenReturn(true);
        when(paymentClient.chargePayment(any(PaymentRequest.class)))
                .thenReturn(PaymentResponse.builder().orderId(501L).status(PaymentResponse.STATUS_SUCCESS).transactionId("txn-claim-1").build());

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        InOrder inOrder = inOrder(orderIdempotencyService, paymentClient);
        inOrder.verify(orderIdempotencyService).claimPaymentAttempt(record.getId());
        inOrder.verify(paymentClient).chargePayment(any(PaymentRequest.class));
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 20. paymentAttemptClaim_whenAlreadyClaimed_deniesSecondAttempt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("20. When payment attempt is already claimed, claim is denied and payment is not dispatched")
    void paymentAttemptClaim_whenAlreadyClaimed_deniesSecondAttempt() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        when(orderIdempotencyService.claimPaymentAttempt(record.getId())).thenReturn(false);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentClient, never()).chargePayment(any());
        verify(productClient, never()).releaseStock(any());
    }

    // -------------------------------------------------------------------------
    // 21. paymentSuccessWithNullOrderId_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("21. Payment SUCCESS with null orderId marks order PAYMENT_UNKNOWN, does not release stock, does not retry")
    void paymentSuccessWithNullOrderId_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse response = PaymentResponse.builder()
                .orderId(null)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-valid-123")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(response);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 22. paymentSuccessWithWrongOrderId_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("22. Payment SUCCESS with wrong orderId marks order PAYMENT_UNKNOWN, does not release stock, does not retry")
    void paymentSuccessWithWrongOrderId_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse response = PaymentResponse.builder()
                .orderId(999L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("txn-valid-123")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(response);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 23. paymentSuccessWithNullTransactionId_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("23. Payment SUCCESS with null transactionId marks order PAYMENT_UNKNOWN, does not release stock, does not retry")
    void paymentSuccessWithNullTransactionId_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse response = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId(null)
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(response);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }

    // -------------------------------------------------------------------------
    // 24. paymentSuccessWithBlankTransactionId_marksOrderPaymentUnknown
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("24. Payment SUCCESS with blank transactionId marks order PAYMENT_UNKNOWN, does not release stock, does not retry")
    void paymentSuccessWithBlankTransactionId_marksOrderPaymentUnknown() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        mockCartAndProductHold(new BigDecimal("100.00"), 1);
        mockOrderPersistence(record, new BigDecimal("100.00"));

        PaymentResponse response = PaymentResponse.builder()
                .orderId(501L)
                .status(PaymentResponse.STATUS_SUCCESS)
                .transactionId("   ")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(response);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_UNKNOWN);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_UNKNOWN));
        verify(productClient, never()).releaseStock(any());
        verify(cartService, never()).clearCart(any());
        verify(paymentClient, times(1)).chargePayment(any());
    }
}
