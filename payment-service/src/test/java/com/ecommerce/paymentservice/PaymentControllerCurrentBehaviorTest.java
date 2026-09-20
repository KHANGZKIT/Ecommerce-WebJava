package com.ecommerce.paymentservice;

import com.ecommerce.paymentservice.controller.PaymentController;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.exception.GlobalExceptionHandler;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerCurrentBehaviorTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void processPayment_withValidRequest_returns201Created() throws Exception {
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
                .transactionId("txn-abc-123")
                .orderId(501L)
                .userId(10L)
                .amount(new BigDecimal("129.99"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .message("Payment processed successfully via CREDIT_CARD")
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionId").value("txn-abc-123"))
                .andExpect(jsonPath("$.orderId").value(501))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).processPayment(any(PaymentRequest.class));
    }

    @Test
    void processPayment_withInvalidAmountOrMissingFields_returns400BadRequest() throws Exception {
        // Zero amount, missing orderId, missing userId, missing paymentMethod
        String invalidJson = """
                {
                    "amount": 0.00
                }
                """;

        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists());
    }

    @Test
    void processRefund_withValidRequest_returns200Ok() throws Exception {
        String requestJson = """
                {
                    "paymentId": 55,
                    "reason": "Customer cancellation"
                }
                """;

        PaymentResponse response = PaymentResponse.builder()
                .id(55L)
                .status(PaymentStatus.REFUNDED)
                .message("Refunded. Reason: Customer cancellation")
                .build();

        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void processRefund_withBlankReasonOrMissingPaymentId_returns400() throws Exception {
        String invalidJson = """
                {
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/v1/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paymentId").exists())
                .andExpect(jsonPath("$.errors.reason").exists());
    }

    @Test
    void getByOrderId_returnsListOfPayments() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(101L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByOrderId(101L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/payments/order/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(101));
    }

    @Test
    void getByUserId_returnsListOfPayments() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(2L)
                .userId(50L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByUserId(50L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/payments/user/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userId").value(50));
    }

    @Test
    void getPaymentById_whenNotFound_returns404() throws Exception {
        when(paymentService.getPaymentById(eq(999L)))
                .thenThrow(new PaymentNotFoundException("Payment not found: 999"));

        mockMvc.perform(get("/api/v1/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payment not found: 999"));
    }
}
