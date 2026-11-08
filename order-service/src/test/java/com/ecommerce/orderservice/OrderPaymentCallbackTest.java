package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.OrderDto;
import com.ecommerce.orderservice.dto.PaymentCallbackRequest;
import com.ecommerce.orderservice.dto.ReleaseStockRequest;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderIdempotencyRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.service.CartService;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentCallbackTest {

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

    @Mock
    private OrderIdempotencyRepository orderIdempotencyRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                cartService,
                orderIdempotencyService,
                orderIdempotencyRepository
        );
    }

    @Test
    @DisplayName("Should confirm order and clear cart on SUCCESS payment callback")
    void shouldConfirmOrderOnSuccessCallback() {
        Long orderId = 101L;
        Long userId = 1L;

        Order pendingOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("15000000"))
                .items(new ArrayList<>())
                .build();

        OrderIdempotency idempotency = OrderIdempotency.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey("idem-key-101")
                .order(pendingOrder)
                .build();

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderIdempotencyRepository.findFirstByOrder_Id(orderId)).thenReturn(Optional.of(idempotency));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentCallbackRequest callback = PaymentCallbackRequest.builder()
                .status("SUCCESS")
                .transactionId("TECHGEAR_TXN_001")
                .gatewayTransactionId("99887766")
                .message("Payment successful")
                .build();

        OrderDto result = orderService.handlePaymentCallback(orderId, callback);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getPaymentTransactionId()).isEqualTo("TECHGEAR_TXN_001");
        verify(cartService).clearCart(userId);
    }

    @Test
    @DisplayName("Should be idempotent when duplicate SUCCESS payment callback arrives")
    void shouldHandleDuplicateSuccessCallbackIdempotently() {
        Long orderId = 101L;
        Long userId = 1L;

        Order confirmedOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.CONFIRMED)
                .paymentTransactionId("TECHGEAR_TXN_001")
                .totalAmount(new BigDecimal("15000000"))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(confirmedOrder));

        PaymentCallbackRequest callback = PaymentCallbackRequest.builder()
                .status("SUCCESS")
                .transactionId("TECHGEAR_TXN_001")
                .build();

        OrderDto result = orderService.handlePaymentCallback(orderId, callback);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartService, never()).clearCart(anyLong());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark PAYMENT_FAILED and release held stock on FAILED payment callback")
    void shouldFailOrderAndReleaseStockOnFailedCallback() {
        Long orderId = 101L;
        Long userId = 1L;

        Order pendingOrder = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("15000000"))
                .items(new ArrayList<>())
                .build();

        OrderIdempotency idempotency = OrderIdempotency.builder()
                .id(1L)
                .userId(userId)
                .idempotencyKey("idem-key-101")
                .order(pendingOrder)
                .build();

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderIdempotencyRepository.findFirstByOrder_Id(orderId)).thenReturn(Optional.of(idempotency));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentCallbackRequest callback = PaymentCallbackRequest.builder()
                .status("FAILED")
                .transactionId("TECHGEAR_TXN_001")
                .message("User cancelled on MoMo")
                .build();

        OrderDto result = orderService.handlePaymentCallback(orderId, callback);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(productClient).releaseStock(argThat(req -> "idem-key-101".equals(req.getCheckoutOperationKey())));
        verify(cartService, never()).clearCart(anyLong());
    }
}
