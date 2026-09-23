package com.ecommerce.paymentservice.security;

import com.ecommerce.paymentservice.controller.PaymentController;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.exception.GlobalExceptionHandler;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.security.Key;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentSecurityBoundaryTest {

    private static final Key SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Key WRONG_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private MockMvc mvc;
    private AnnotationConfigWebApplicationContext context;
    private PaymentService paymentService;

    @Configuration
    @EnableWebMvc
    @Import({PaymentSecurityConfig.class, PaymentController.class, GlobalExceptionHandler.class})
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        PaymentJwtVerifier verifier() {
            return new PaymentJwtVerifier(Encoders.BASE64.encode(SIGNING_KEY.getEncoded()));
        }

        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
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

        paymentService = context.getBean(PaymentService.class);

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

    private static String validBearerToken(long userId, String role) {
        return "Bearer " + createToken(
                userId,
                "user" + userId + "@example.com",
                role,
                new Date(System.currentTimeMillis() + 3600_000),
                SIGNING_KEY,
                SignatureAlgorithm.HS256
        );
    }

    @Test
    void charge_withoutToken_succeedsForInternalCall() throws Exception {
        String requestJson = """
                {
                    "orderId": 501,
                    "userId": 10,
                    "amount": 129.99,
                    "paymentMethod": "CREDIT_CARD"
                }
                """;

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionId("txn-123")
                .orderId(501L)
                .userId(10L)
                .amount(new BigDecimal("129.99"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getPaymentById_withoutToken_returns401Unauthorized() throws Exception {
        mvc.perform(get("/api/v1/payments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getPaymentById_withInvalidToken_returns401Unauthorized() throws Exception {
        String badToken = "Bearer " + createToken(
                10L, "hacker", "CUSTOMER",
                new Date(System.currentTimeMillis() + 3600_000),
                WRONG_KEY,
                SignatureAlgorithm.HS256
        );

        mvc.perform(get("/api/v1/payments/1").header("Authorization", badToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getPaymentById_customerAccessingOwnPayment_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .userId(10L)
                .orderId(501L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mvc.perform(get("/api/v1/payments/1")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void getPaymentById_customerAccessingOtherUserPayment_returns403() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .userId(99L)
                .orderId(501L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mvc.perform(get("/api/v1/payments/1")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to payment"));
    }

    @Test
    void getPaymentById_adminAccessingOtherUserPayment_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .userId(99L)
                .orderId(501L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mvc.perform(get("/api/v1/payments/1")
                        .header("Authorization", validBearerToken(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(99));
    }

    @Test
    void getByUserId_customerAccessingOwnPayments_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(2L)
                .userId(10L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByUserId(10L)).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/payments/user/10")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userId").value(10));
    }

    @Test
    void getByUserId_customerAccessingOtherUserPayments_returns403() throws Exception {
        mvc.perform(get("/api/v1/payments/user/99")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to user payments"));
    }

    @Test
    void getByUserId_adminAccessingOtherUserPayments_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(2L)
                .userId(99L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByUserId(99L)).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/payments/user/99")
                        .header("Authorization", validBearerToken(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userId").value(99));
    }

    @Test
    void getByOrderId_customerAccessingOwnOrderPayments_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(501L)
                .userId(10L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByOrderId(501L)).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/payments/order/501")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(501));
    }

    @Test
    void getByOrderId_customerAccessingOtherUserOrderPayments_returns403() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(501L)
                .userId(99L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByOrderId(501L)).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/payments/order/501")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied to order payments"));
    }

    @Test
    void getByOrderId_adminAccessingOtherUserOrderPayments_returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(501L)
                .userId(99L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByOrderId(501L)).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/payments/order/501")
                        .header("Authorization", validBearerToken(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(99));
    }

    @Test
    void refund_withoutToken_returns401Unauthorized() throws Exception {
        String requestJson = """
                {
                    "paymentId": 55,
                    "reason": "Customer cancellation"
                }
                """;

        mvc.perform(post("/api/v1/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void refund_byCustomer_returns403Forbidden() throws Exception {
        String requestJson = """
                {
                    "paymentId": 55,
                    "reason": "Customer cancellation"
                }
                """;

        mvc.perform(post("/api/v1/payments/refund")
                        .header("Authorization", validBearerToken(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void refund_byAdmin_returns200Ok() throws Exception {
        String requestJson = """
                {
                    "paymentId": 55,
                    "reason": "Customer cancellation"
                }
                """;

        PaymentResponse response = PaymentResponse.builder()
                .id(55L)
                .status(PaymentStatus.REFUNDED)
                .message("Refunded successfully")
                .build();

        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/payments/refund")
                        .header("Authorization", validBearerToken(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
