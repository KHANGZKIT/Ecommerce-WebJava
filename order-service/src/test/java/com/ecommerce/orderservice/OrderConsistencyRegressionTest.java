package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.controller.OrderController;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.GlobalExceptionHandler;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.model.PaymentMethod;
import com.ecommerce.orderservice.repository.OrderIdempotencyRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.orderservice.security.OrderPrincipal;
import com.ecommerce.orderservice.service.CartService;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class OrderConsistencyRegressionTest {

    @Mock
    private OrderService orderService;

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(long userId, String role) {
        OrderPrincipal principal = new OrderPrincipal(userId, "user" + userId + "@example.com", role);
        var authorities = "ADMIN".equalsIgnoreCase(role)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                : List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * REG-TEST-02: Call GET /api/v1/orders with admin token.
     * Assert HTTP 200 and order content is present.
     */
    @Test
    @DisplayName("REG-TEST-02: Admin user retrieves order page from GET /api/v1/orders successfully (HTTP 200)")
    void testAdminOrderListingEndpoint() throws Exception {
        setAuthenticatedUser(1L, "ADMIN");

        OrderDto order19 = OrderDto.builder()
                .id(19L)
                .userId(33L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("8990000.00"))
                .shippingAddress("123 Tech Avenue, Ha Noi")
                .paymentTransactionId("0b918c0a-33c5-4296-8586-ba4ec8c66634")
                .createdAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();

        when(orderService.getAllOrders(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order19), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(19))
                .andExpect(jsonPath("$.content[0].userId").value(33))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(8990000.00))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).getAllOrders(eq(null), eq(null), any(Pageable.class));
    }

    /**
     * REG-TEST-03: Call GET /api/v1/orders with standard customer token.
     * Assert HTTP 403 Forbidden.
     */
    @Test
    @DisplayName("REG-TEST-03: Customer user calling GET /api/v1/orders is rejected with HTTP 403 Forbidden")
    void testAdminOrderListingForbiddenForNonAdmin() throws Exception {
        setAuthenticatedUser(33L, "CUSTOMER");

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access required to view all orders"));

        verifyNoInteractions(orderService);
    }

    /**
     * REG-TEST-05: Hold stock for Product. Simulate payment failure.
     * Verify order status is PAYMENT_FAILED and productClient.releaseStock is invoked.
     */
    @Test
    @DisplayName("REG-TEST-05: Inventory compensation releases held stock when payment fails during checkout")
    void testInventoryCompensationOnPaymentFailure() {
        OrderService realOrderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                cartService,
                orderIdempotencyService,
                orderIdempotencyRepository
        );

        Long userId = 33L;
        String idempotencyKey = "test-idem-failure-key-001";
        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Street, District 1, HCMC")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        // 1. Mock Idempotency record
        OrderIdempotency idempotencyRecord = OrderIdempotency.builder()
                .id(50L)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .build();
        when(orderIdempotencyService.getOrCreate(userId, idempotencyKey, request)).thenReturn(idempotencyRecord);

        // 2. Mock Cart
        CartItemDto cartItem = CartItemDto.builder()
                .productId(468L)
                .quantity(1)
                .unitPrice(new BigDecimal("8990000.00"))
                .build();
        CartDto cart = CartDto.builder()
                .userId(userId)
                .items(List.of(cartItem))
                .totalPrice(new BigDecimal("8990000.00"))
                .build();
        when(cartService.getCartByUserId(userId)).thenReturn(cart);

        // 3. Mock Stock Hold success
        HeldItemDto heldItem = HeldItemDto.builder()
                .productId(468L)
                .sku("MN-LG-27UP850N")
                .name("Man hinh LG 27UP850N-W")
                .price(new BigDecimal("8990000.00"))
                .quantity(1)
                .build();
        HoldStockResponse holdResponse = HoldStockResponse.builder()
                .items(List.of(heldItem))
                .build();
        when(productClient.holdStock(any(HoldStockRequest.class))).thenReturn(holdResponse);

        // 4. Mock Order Persistence
        when(orderIdempotencyService.persistPendingOrderAndLink(any(Order.class), anyList(), eq(50L), eq(idempotencyKey)))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(99L);
                    return o;
                });

        when(orderIdempotencyService.claimPaymentAttempt(50L)).thenReturn(true);

        // 5. Mock Payment Failure (payment-service rejected unintegrated method CREDIT_CARD)
        PaymentResponse failedPaymentResp = PaymentResponse.builder()
                .orderId(99L)
                .status(PaymentResponse.STATUS_FAILED)
                .message("Payment rejected: unintegrated payment method")
                .build();
        when(paymentClient.chargePayment(any(PaymentRequest.class))).thenReturn(failedPaymentResp);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Execute checkout
        Order result = realOrderService.checkout(userId, idempotencyKey, request, "Bearer dummy-token");

        // Verify status
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);

        // Verify inventory compensation: releaseStock MUST be called with the idempotencyKey
        ArgumentCaptor<ReleaseStockRequest> releaseCaptor = ArgumentCaptor.forClass(ReleaseStockRequest.class);
        verify(productClient).releaseStock(releaseCaptor.capture());
        assertThat(releaseCaptor.getValue().getCheckoutOperationKey()).isEqualTo(idempotencyKey);
    }
}
