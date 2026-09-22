package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.PaymentClient;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestrationPhase2Test {

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
    private final String idempotencyKey = "test-key-42";
    private final String bearerToken = "valid-customer-jwt-token";

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
                .notes("Handle with care")
                .build();
    }

    private OrderIdempotency mockNewIdempotencyRecord() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(1001L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint("mock-fingerprint-123")
                .build();
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any(CheckoutRequest.class)))
                .thenReturn(record);
        return record;
    }

    // -------------------------------------------------------------------------
    // 1. empty cart rejected; no Product/Order save
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. Empty cart rejected with 400 Bad Request; no Product HOLD or Order persistence")
    void checkout_whenCartIsEmpty_rejectsCheckoutWithBadRequest_andDoesNotCallProductOrSaveOrder() {
        mockNewIdempotencyRecord();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(Collections.emptyList()).build());

        assertThatThrownBy(() -> orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");

        verify(cartService).getCartByUserId(userId);
        verify(productClient, never()).holdStock(any());
        verify(orderRepository, never()).save(any());
        verify(orderIdempotencyService, never()).persistPendingOrderAndLink(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // 2. Cart called with verified Bearer token
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Cart is fetched by userId from internal CartService")
    void checkout_fetchesCartUsingVerifiedBearerToken() {
        OrderIdempotency record = mockNewIdempotencyRecord();

        CartItemDto item = CartItemDto.builder().productId(10L).quantity(2).build();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(item)).build());

        HeldItemDto heldItem = new HeldItemDto(10L, "Keyboard", "KB-01", new BigDecimal("45.00"), 2);
        HoldStockResponse holdResponse = new HoldStockResponse(List.of(heldItem));
        when(productClient.holdStock(any(HoldStockRequest.class))).thenReturn(holdResponse);

        Order savedOrder = Order.builder().id(501L).userId(userId).status(OrderStatus.PENDING).totalAmount(new BigDecimal("90.00")).build();
        when(orderIdempotencyService.persistPendingOrderAndLink(any(Order.class), anyList(), eq(record.getId()), eq(idempotencyKey)))
                .thenReturn(savedOrder);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        verify(cartService).getCartByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // 3. Product HOLD gets cart quantities + idempotency key
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Product HOLD is invoked with cart items (quantities only) and checkoutOperationKey = idempotencyKey")
    void checkout_holdsProductStockWithCartQuantitiesAndIdempotencyKey() {
        OrderIdempotency record = mockNewIdempotencyRecord();

        CartItemDto item1 = CartItemDto.builder().productId(101L).quantity(3).build();
        CartItemDto item2 = CartItemDto.builder().productId(102L).quantity(1).build();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(item1, item2)).build());

        HeldItemDto h1 = new HeldItemDto(101L, "Mouse", "MS-01", new BigDecimal("15.00"), 3);
        HeldItemDto h2 = new HeldItemDto(102L, "Pad", "PD-01", new BigDecimal("5.00"), 1);
        HoldStockResponse holdResponse = new HoldStockResponse(List.of(h1, h2));
        when(productClient.holdStock(any(HoldStockRequest.class))).thenReturn(holdResponse);

        Order savedOrder = Order.builder().id(502L).userId(userId).status(OrderStatus.PENDING).build();
        when(orderIdempotencyService.persistPendingOrderAndLink(any(Order.class), anyList(), eq(record.getId()), eq(idempotencyKey)))
                .thenReturn(savedOrder);

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        ArgumentCaptor<HoldStockRequest> captor = ArgumentCaptor.forClass(HoldStockRequest.class);
        verify(productClient).holdStock(captor.capture());

        HoldStockRequest captured = captor.getValue();
        assertThat(captured.getCheckoutOperationKey()).isEqualTo(idempotencyKey);
        assertThat(captured.getItems()).hasSize(2);
        assertThat(captured.getItems().get(0).getProductId()).isEqualTo(101L);
        assertThat(captured.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(captured.getItems().get(1).getProductId()).isEqualTo(102L);
        assertThat(captured.getItems().get(1).getQuantity()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 4. Order uses Product authoritative prices + correct total
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Order items and totalAmount are computed strictly from Product authoritative prices × quantities")
    void checkout_createsOrderUsingProductAuthoritativePriceAndCalculatesTotal() {
        OrderIdempotency record = mockNewIdempotencyRecord();

        CartItemDto item1 = CartItemDto.builder().productId(10L).quantity(2).build();
        CartItemDto item2 = CartItemDto.builder().productId(20L).quantity(3).build();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(item1, item2)).build());

        // Authoritative product prices: 100.00 and 25.50
        HeldItemDto h1 = new HeldItemDto(10L, "Monitor", "MN-01", new BigDecimal("100.00"), 2);
        HeldItemDto h2 = new HeldItemDto(20L, "HDMI Cable", "CB-01", new BigDecimal("25.50"), 3);
        HoldStockResponse holdResponse = new HoldStockResponse(List.of(h1, h2));
        when(productClient.holdStock(any(HoldStockRequest.class))).thenReturn(holdResponse);

        // Expected total = 100.00*2 + 25.50*3 = 200.00 + 76.50 = 276.50
        BigDecimal expectedTotal = new BigDecimal("276.50");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);

        when(orderIdempotencyService.persistPendingOrderAndLink(orderCaptor.capture(), itemsCaptor.capture(), eq(record.getId()), eq(idempotencyKey)))
                .thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getTotalAmount()).isEqualByComparingTo(expectedTotal);

        List<OrderItem> capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedItems.get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(capturedItems.get(1).getUnitPrice()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(capturedItems.get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("76.50"));
    }

    // -------------------------------------------------------------------------
    // 5. Order status PENDING
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Created Order has status PENDING")
    void checkout_createsOrderWithPendingStatus() {
        OrderIdempotency record = mockNewIdempotencyRecord();

        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(CartItemDto.builder().productId(1L).quantity(1).build())).build());
        when(productClient.holdStock(any())).thenReturn(new HoldStockResponse(List.of(
                new HeldItemDto(1L, "Widget", "WD-01", new BigDecimal("10.00"), 1)
        )));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderIdempotencyService.persistPendingOrderAndLink(orderCaptor.capture(), anyList(), eq(record.getId()), eq(idempotencyKey)))
                .thenAnswer(inv -> inv.getArgument(0));

        orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // 6. Authenticated principal userId used
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Order userId is set strictly from the verified authenticated principal")
    void checkout_usesAuthenticatedUserIdFromPrincipal() {
        Long authenticatedPrincipalId = 99999L;
        OrderIdempotency record = OrderIdempotency.builder()
                .id(2002L)
                .userId(authenticatedPrincipalId)
                .idempotencyKey(idempotencyKey)
                .build();
        when(orderIdempotencyService.getOrCreate(eq(authenticatedPrincipalId), eq(idempotencyKey), any()))
                .thenReturn(record);

        when(cartService.getCartByUserId(authenticatedPrincipalId)).thenReturn(CartDto.builder().userId(authenticatedPrincipalId).items(List.of(CartItemDto.builder().productId(1L).quantity(1).build())).build());
        when(productClient.holdStock(any())).thenReturn(new HoldStockResponse(List.of(
                new HeldItemDto(1L, "Widget", "WD-01", new BigDecimal("10.00"), 1)
        )));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderIdempotencyService.persistPendingOrderAndLink(orderCaptor.capture(), anyList(), eq(record.getId()), eq(idempotencyKey)))
                .thenAnswer(inv -> inv.getArgument(0));

        orderService.checkout(authenticatedPrincipalId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(orderCaptor.getValue().getUserId()).isEqualTo(authenticatedPrincipalId);
    }

    // -------------------------------------------------------------------------
    // 7. same idempotency key replay returns existing Order without Cart/Product
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Replaying same key with same payload returns existing Order without calling Cart or Product")
    void checkout_whenSameIdempotencyKeyReplayed_recoversExistingOrderWithoutCallingProductOrCart() {
        Order existingOrder = Order.builder()
                .id(777L)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("150.00"))
                .build();

        OrderIdempotency recordWithOrder = OrderIdempotency.builder()
                .id(1001L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .order(existingOrder)
                .build();

        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any()))
                .thenReturn(recordWithOrder);

        Order result = orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(777L);

        verify(cartService, never()).getCartByUserId(any());
        verify(productClient, never()).holdStock(any());
        verify(orderIdempotencyService, never()).persistPendingOrderAndLink(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // 8. same key + different fingerprint -> 409
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Same key with conflicting fingerprint throws 409 Conflict without Cart or Product calls")
    void checkout_whenSameKeyWithDifferentFingerprint_throwsConflict409() {
        when(orderIdempotencyService.getOrCreate(eq(userId), eq(idempotencyKey), any()))
                .thenThrow(new ApiException("Idempotency key conflict: payload does not match existing request", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(cartService, never()).getCartByUserId(any());
        verify(productClient, never()).holdStock(any());
    }

    // -------------------------------------------------------------------------
    // 9. Product HOLD failure -> no Order
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Downstream Product HOLD failure throws original error and persists no Order")
    void checkout_whenProductHoldFails_createsNoOrderAndPreservesProductError() {
        mockNewIdempotencyRecord();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(CartItemDto.builder().productId(1L).quantity(100).build())).build());

        when(productClient.holdStock(any()))
                .thenThrow(new ApiException("Insufficient stock for product: 1", HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderIdempotencyService, never()).persistPendingOrderAndLink(any(), any(), any(), any());
        verify(productClient, never()).releaseStock(any());
    }

    // -------------------------------------------------------------------------
    // 10. Order persistence failure after HOLD -> Product RELEASE attempted
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Order persistence failure after successful HOLD attempts Product RELEASE and rethrows original exception")
    void checkout_whenOrderPersistenceFailsAfterHold_attemptsProductReleaseAndPropagatesOriginalError() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(CartItemDto.builder().productId(1L).quantity(1).build())).build());
        when(productClient.holdStock(any())).thenReturn(new HoldStockResponse(List.of(
                new HeldItemDto(1L, "Widget", "WD-01", new BigDecimal("10.00"), 1)
        )));

        when(orderIdempotencyService.persistPendingOrderAndLink(any(), any(), eq(record.getId()), eq(idempotencyKey)))
                .thenThrow(new RuntimeException("Database constraint violation during order save"));

        assertThatThrownBy(() -> orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database constraint violation during order save");

        ArgumentCaptor<ReleaseStockRequest> releaseCaptor = ArgumentCaptor.forClass(ReleaseStockRequest.class);
        verify(productClient).releaseStock(releaseCaptor.capture());
        assertThat(releaseCaptor.getValue().getCheckoutOperationKey()).isEqualTo(idempotencyKey);
    }

    // -------------------------------------------------------------------------
    // 11. Product RELEASE compensation failure -> original persistence error preserved
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. If Product RELEASE fails during compensation, the original Order persistence error is preserved")
    void checkout_whenProductReleaseFailsDuringCompensation_propagatesOriginalPersistenceError() {
        OrderIdempotency record = mockNewIdempotencyRecord();
        when(cartService.getCartByUserId(userId)).thenReturn(CartDto.builder().userId(userId).items(List.of(CartItemDto.builder().productId(1L).quantity(1).build())).build());
        when(productClient.holdStock(any())).thenReturn(new HoldStockResponse(List.of(
                new HeldItemDto(1L, "Widget", "WD-01", new BigDecimal("10.00"), 1)
        )));

        when(orderIdempotencyService.persistPendingOrderAndLink(any(), any(), eq(record.getId()), eq(idempotencyKey)))
                .thenThrow(new RuntimeException("Fatal DB error saving order"));

        when(productClient.releaseStock(any()))
                .thenThrow(new ApiException("Product service release unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> orderService.checkout(userId, idempotencyKey, sampleRequest, bearerToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fatal DB error saving order");

        verify(productClient).releaseStock(any());
    }
}
