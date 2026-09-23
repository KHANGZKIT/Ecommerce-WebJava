package com.ecommerce.orderservice.security;

import com.ecommerce.orderservice.controller.OrderController;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderDto;
import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.exception.GlobalExceptionHandler;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderSecurityBoundaryTest {

    private static final Key SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Key WRONG_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private MockMvc mvc;
    private AnnotationConfigWebApplicationContext context;
    private OrderService orderService;
    private OrderIdempotencyService orderIdempotencyService;

    @Configuration
    @EnableWebMvc
    @Import({OrderSecurityConfig.class, OrderController.class, GlobalExceptionHandler.class})
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        OrderJwtVerifier verifier() {
            return new OrderJwtVerifier(Encoders.BASE64.encode(SIGNING_KEY.getEncoded()));
        }

        @Bean
        OrderService orderService() {
            return mock(OrderService.class);
        }

        @Bean
        OrderIdempotencyService orderIdempotencyService() {
            return mock(OrderIdempotencyService.class);
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new PageableHandlerMethodArgumentResolver());
        }

        @Override
        public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
            for (HttpMessageConverter<?> converter : converters) {
                if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                    jacksonConverter.getObjectMapper()
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();

        orderService = context.getBean(OrderService.class);
        orderIdempotencyService = context.getBean(OrderIdempotencyService.class);

        when(orderIdempotencyService.getOrCreate(anyLong(), any(), any(CheckoutRequest.class)))
                .thenAnswer(inv -> OrderIdempotency.builder()
                        .userId(inv.getArgument(0))
                        .idempotencyKey(inv.getArgument(1))
                        .requestFingerprint("mock-fingerprint")
                        .build());

        Filter securityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);

        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    private static String createToken(Object userId, String subject, Date expiration, Key key, SignatureAlgorithm alg) {
        return createToken(userId, subject, "CUSTOMER", expiration, key, alg);
    }

    private static String createToken(Object userId, String subject, String role, Date expiration, Key key, SignatureAlgorithm alg) {
        var builder = Jwts.builder()
                .setSubject(subject)
                .setExpiration(expiration)
                .signWith(key, alg);
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    private static final String VALID_CHECKOUT_REQUEST_JSON = """
            {
                "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                "paymentMethod": "CREDIT_CARD",
                "notes": "Please leave the package at the front door."
            }
            """;

    private static String validBearerToken(long userId) {
        return validBearerToken(userId, "CUSTOMER");
    }

    private static String validBearerToken(long userId, String role) {
        Date future = new Date(System.currentTimeMillis() + 3600000);
        return "Bearer " + createToken(userId, "user" + userId + "@example.com", role, future, SIGNING_KEY, SignatureAlgorithm.HS256);
    }

    @Test
    void checkout_withValidJwtAndRequestAndIdempotencyKey_acceptsRequest() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(42))
                .andExpect(jsonPath("$.idempotencyKey").value("test-key-42"))
                .andExpect(jsonPath("$.status").value("CHECKOUT_BOUNDARY_ESTABLISHED"));
    }

    @Test
    void checkout_withValidJwtAndRequestWithoutNotes_acceptsRequest() throws Exception {
        String requestWithoutNotes = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                    "paymentMethod": "UPI"
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-upi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithoutNotes))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(42))
                .andExpect(jsonPath("$.idempotencyKey").value("test-key-upi"))
                .andExpect(jsonPath("$.status").value("CHECKOUT_BOUNDARY_ESTABLISHED"));
    }

    @Test
    void checkout_withMissingAuthorizationHeader_isRejectedWith401() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Valid bearer authentication required"));
    }

    @Test
    void checkout_withMalformedAuthorizationHeader_isRejectedWith401() throws Exception {
        // Not starting with Bearer
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Empty bearer token
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Whitespace in bearer token
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer token with spaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void checkout_withInvalidSignature_isRejectedWith401() throws Exception {
        Date future = new Date(System.currentTimeMillis() + 3600000);
        String tamperedToken = "Bearer " + createToken(42L, "user@example.com", future, WRONG_KEY, SignatureAlgorithm.HS256);

        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void checkout_withExpiredToken_isRejectedWith401() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 3600000);
        String expiredToken = "Bearer " + createToken(42L, "user@example.com", past, SIGNING_KEY, SignatureAlgorithm.HS256);

        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void checkout_withMissingIdempotencyKeyHeader_isRejectedWith400() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required and must not be blank"));
    }

    @Test
    void checkout_withBlankIdempotencyKeyHeader_isRejectedWith400() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required and must not be blank"));
    }

    @Test
    void checkout_withMissingRequestBody_isRejectedWith400() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_withInvalidPaymentMethod_isRejectedWith400() throws Exception {
        String invalidPaymentMethodJson = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                    "paymentMethod": "BITCOIN"
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPaymentMethodJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_withMissingPaymentMethod_isRejectedWith400() throws Exception {
        String missingPaymentMethodJson = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081"
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingPaymentMethodJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.paymentMethod").value("Payment method is required"));
    }

    @Test
    void checkout_withMissingOrBlankShippingAddress_isRejectedWith400() throws Exception {
        String blankAddressJson = """
                {
                    "shippingAddress": "   ",
                    "paymentMethod": "CREDIT_CARD"
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "test-key-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankAddressJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_withRequestContainingUserId_isRejectedAsUnknownField() throws Exception {
        String bodyWithUserId = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "userId": 888
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(77L))
                        .header("Idempotency-Key", "test-key-77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_withRequestContainingItems_isRejectedAsUnknownField() throws Exception {
        String bodyWithItems = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(77L))
                        .header("Idempotency-Key", "test-key-77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithItems))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_withRequestContainingPriceOrTotal_isRejectedAsUnknownField() throws Exception {
        String bodyWithPrice = """
                {
                    "shippingAddress": "123 Main Street, Tech Park, Hyderabad, Telangana, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "totalAmount": 100.00
                }
                """;
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(77L))
                        .header("Idempotency-Key", "test-key-77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithPrice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void checkout_derivesUserIdStrictlyFromJwt_ignoringGatewayHeaders() throws Exception {
        // Verified JWT contains userId = 101; untrusted gateway headers attempt to spoof userId = 999
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(101L))
                        .header("Idempotency-Key", "test-key-101")
                        .header("X-User-Id", "999")
                        .header("X-User-Email", "spoofed@attacker.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticatedUserId").value(101))
                .andExpect(jsonPath("$.idempotencyKey").value("test-key-101"))
                .andExpect(jsonPath("$.status").value("CHECKOUT_BOUNDARY_ESTABLISHED"));
    }

    @Test
    void legacyCreateOrder_withoutJwt_remainsPubliclyAccessible() throws Exception {
        // Demonstrates backward compatibility: existing explicit-item endpoint does NOT require JWT
        String requestJson = """
                {
                    "userId": 500,
                    "shippingAddress": "123 Main Street, Innovation Hub, Tech City, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 1
                        }
                    ]
                }
                """;

        OrderDto dto = OrderDto.builder()
                .id(999L)
                .userId(500L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(dto);

        mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(999))
                .andExpect(jsonPath("$.userId").value(500));
    }

    @Test
    void legacyCreateOrder_withInvalidJwt_isRejectedWith401() throws Exception {
        // Security requirement: invalid supplied bearer token is rejected even on public routes
        mvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer invalid-tampered-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void orderSecurityUtils_extractsAuthenticatedPrincipalAndUserIdDirectly() {
        OrderPrincipal principal = new OrderPrincipal(12345L, "test@example.com");
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, java.util.List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(OrderSecurityUtils.getAuthenticatedPrincipal()).isEqualTo(principal);
        assertThat(OrderSecurityUtils.getAuthenticatedUserId()).isEqualTo(12345L);

        SecurityContextHolder.clearContext();
        assertThatThrownBy(OrderSecurityUtils::getAuthenticatedPrincipal)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authenticated OrderPrincipal not found");
        assertThatThrownBy(OrderSecurityUtils::getAuthenticatedUserId)
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void checkout_withKeyExceeding128Characters_isRejectedWith400() throws Exception {
        String longKey = "k".repeat(129);
        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", longKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header must not exceed 128 characters"));
    }

    @Test
    void checkout_withConflictingPayload_returns409() throws Exception {
        when(orderIdempotencyService.getOrCreate(anyLong(), eq("conflict-key"), any(CheckoutRequest.class)))
                .thenThrow(new ApiException("Idempotency key conflict: payload does not match existing request for key: conflict-key", HttpStatus.CONFLICT));

        mvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", validBearerToken(42L))
                        .header("Idempotency-Key", "conflict-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CHECKOUT_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Idempotency key conflict: payload does not match existing request for key: conflict-key"));
    }

    @Test
    void customer_canAccessOwnOrder_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(10L)).thenReturn(dto);

        mvc.perform(get("/api/v1/orders/10")
                        .header("Authorization", validBearerToken(101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(101));
    }

    @Test
    void customer_cannotAccessAnotherUserOrder_returns403() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(10L)).thenReturn(dto);

        mvc.perform(get("/api/v1/orders/10")
                        .header("Authorization", validBearerToken(102L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to order 10"));
    }

    @Test
    void admin_canAccessAnyOrder_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(10L)).thenReturn(dto);

        mvc.perform(get("/api/v1/orders/10")
                        .header("Authorization", validBearerToken(999L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(101));
    }

    @Test
    void customer_canAccessOwnOrderHistory_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 10);
        when(orderService.getOrdersByUserId(eq(101L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(dto), pageRequest, 1));

        mvc.perform(get("/api/v1/orders/user/101")
                        .header("Authorization", validBearerToken(101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].userId").value(101));
    }

    @Test
    void customer_cannotAccessAnotherUserOrderHistory_returns403() throws Exception {
        mvc.perform(get("/api/v1/orders/user/101")
                        .header("Authorization", validBearerToken(102L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to user orders"));
    }

    @Test
    void admin_canAccessAnyUserOrderHistory_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, 10);
        when(orderService.getOrdersByUserId(eq(101L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(dto), pageRequest, 1));

        mvc.perform(get("/api/v1/orders/user/101")
                        .header("Authorization", validBearerToken(999L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void customer_canCancelOwnOrder_returns200() throws Exception {
        OrderDto existingDto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        OrderDto cancelledDto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CANCELLED).build();
        when(orderService.getOrderById(10L)).thenReturn(existingDto);
        when(orderService.cancelOrder(10L)).thenReturn(cancelledDto);

        mvc.perform(post("/api/v1/orders/10/cancel")
                        .header("Authorization", validBearerToken(101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void customer_cannotCancelAnotherUserOrder_returns403() throws Exception {
        OrderDto existingDto = OrderDto.builder().id(10L).userId(101L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(10L)).thenReturn(existingDto);

        mvc.perform(post("/api/v1/orders/10/cancel")
                        .header("Authorization", validBearerToken(102L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to cancel order 10"));
    }

    @Test
    void customer_cannotUpdateOrderStatus_returns403() throws Exception {
        mvc.perform(patch("/api/v1/orders/10/status?status=SHIPPED")
                        .header("Authorization", validBearerToken(101L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canUpdateOrderStatus_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(10L).status(OrderStatus.SHIPPED).build();
        when(orderService.updateOrderStatus(10L, OrderStatus.SHIPPED)).thenReturn(dto);

        mvc.perform(patch("/api/v1/orders/10/status?status=SHIPPED")
                        .header("Authorization", validBearerToken(999L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void customer_cannotSpoofUserIdOnLegacyCreateOrder_bindsToPrincipal() throws Exception {
        String requestJson = """
                {
                    "userId": 999,
                    "shippingAddress": "123 Main Street, Tech City, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 1
                        }
                    ]
                }
                """;

        OrderDto dto = OrderDto.builder()
                .id(55L)
                .userId(101L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(dto);

        mvc.perform(post("/api/v1/orders")
                        .header("Authorization", validBearerToken(101L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(101));

        verify(orderService).createOrder(argThat(req -> req.getUserId().equals(101L)));
    }
}

