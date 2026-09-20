package com.ecommerce.paymentservice;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceCurrentBehaviorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest createSampleRequest(Long orderId, Long userId, BigDecimal amount) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setUserId(userId);
        request.setAmount(amount);
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        return request;
    }

    @Test
    void processPayment_persistsPaymentWithGeneratedUuidTransactionId() {
        PaymentRequest request = createSampleRequest(101L, 1L, new BigDecimal("49.99"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo("49.99");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(response.getTransactionId()).isNotBlank();
        // Assert transactionId is a valid UUID
        UUID.fromString(response.getTransactionId());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(101L);
        assertThat(saved.getTransactionId()).isEqualTo(response.getTransactionId());
        assertThat(saved.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED);
    }

    @Test
    void processPayment_resultsInEitherSuccessOrFailedWithAppropriateMessage() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Run multiple times to observe message contract for whichever outcome RANDOM produces
        boolean sawSuccess = false;
        boolean sawFailed = false;

        for (int i = 0; i < 50; i++) {
            PaymentRequest req = createSampleRequest(200L + i, 5L, new BigDecimal("10.00"));
            PaymentResponse resp = paymentService.processPayment(req);

            if (resp.getStatus() == PaymentStatus.SUCCESS) {
                sawSuccess = true;
                assertThat(resp.getMessage()).isEqualTo("Payment processed successfully via CREDIT_CARD");
            } else if (resp.getStatus() == PaymentStatus.FAILED) {
                sawFailed = true;
                assertThat(resp.getMessage()).isEqualTo("Payment declined by bank. Please try again.");
            } else {
                org.junit.jupiter.api.Assertions.fail("Unexpected payment status: " + resp.getStatus());
            }
        }

        // Over 50 iterations with 90% success rate, we should observe at least success
        assertThat(sawSuccess).isTrue();
    }

    @Test
    void processPayment_whenDuplicateOrderCharge_createsDistinctPaymentRecordsWithoutDeduplication() {
        // Documenting current behavior: there is no idempotency check or duplicate charge prevention
        Long sharedOrderId = 888L;
        PaymentRequest firstCharge = createSampleRequest(sharedOrderId, 12L, new BigDecimal("100.00"));
        PaymentRequest secondCharge = createSampleRequest(sharedOrderId, 12L, new BigDecimal("100.00"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            return p;
        });

        PaymentResponse firstResp = paymentService.processPayment(firstCharge);
        PaymentResponse secondResp = paymentService.processPayment(secondCharge);

        // Verify two separate calls to repository save
        verify(paymentRepository, times(2)).save(any(Payment.class));

        // Verify two different transaction IDs generated for the same orderId
        assertThat(firstResp.getOrderId()).isEqualTo(sharedOrderId);
        assertThat(secondResp.getOrderId()).isEqualTo(sharedOrderId);
        assertThat(firstResp.getTransactionId()).isNotEqualTo(secondResp.getTransactionId());
    }

    @Test
    void processRefund_whenPaymentIsSuccess_transitionsToRefunded() {
        Payment successfulPayment = Payment.builder()
                .id(50L)
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("75.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .message("Payment processed successfully")
                .build();

        when(paymentRepository.findById(50L)).thenReturn(Optional.of(successfulPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setPaymentId(50L);
        refundRequest.setReason("Item damaged upon arrival");

        PaymentResponse response = paymentService.processRefund(refundRequest);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getMessage()).isEqualTo("Refunded. Reason: Item damaged upon arrival");

        verify(paymentRepository).save(successfulPayment);
    }

    @Test
    void processRefund_whenPaymentNotSuccess_throwsPaymentProcessingException() {
        Payment failedPayment = Payment.builder().id(51L).status(PaymentStatus.FAILED).build();
        Payment pendingPayment = Payment.builder().id(52L).status(PaymentStatus.PENDING).build();
        Payment refundedPayment = Payment.builder().id(53L).status(PaymentStatus.REFUNDED).build();

        when(paymentRepository.findById(51L)).thenReturn(Optional.of(failedPayment));
        when(paymentRepository.findById(52L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.findById(53L)).thenReturn(Optional.of(refundedPayment));

        RefundRequest req = new RefundRequest();
        req.setReason("Test");

        req.setPaymentId(51L);
        assertThatThrownBy(() -> paymentService.processRefund(req))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Cannot refund payment in status: FAILED");

        req.setPaymentId(52L);
        assertThatThrownBy(() -> paymentService.processRefund(req))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Cannot refund payment in status: PENDING");

        req.setPaymentId(53L);
        assertThatThrownBy(() -> paymentService.processRefund(req))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Cannot refund payment in status: REFUNDED");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processRefund_whenPaymentNotFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        RefundRequest req = new RefundRequest();
        req.setPaymentId(999L);
        req.setReason("Customer request");

        assertThatThrownBy(() -> paymentService.processRefund(req))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found: 999");
    }

    @Test
    void getPaymentsByOrderId_returnsMultiplePaymentsIfOrderChargedMultipleTimes() {
        Payment p1 = Payment.builder().id(1L).orderId(777L).userId(10L).amount(new BigDecimal("50.00")).status(PaymentStatus.FAILED).build();
        Payment p2 = Payment.builder().id(2L).orderId(777L).userId(10L).amount(new BigDecimal("50.00")).status(PaymentStatus.SUCCESS).build();

        when(paymentRepository.findByOrderId(777L)).thenReturn(List.of(p1, p2));

        List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(777L);

        assertThat(payments).hasSize(2);
        assertThat(payments.get(0).getId()).isEqualTo(1L);
        assertThat(payments.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getPaymentsByUserId_returnsAllPaymentsForUser() {
        Payment p1 = Payment.builder().id(1L).orderId(101L).userId(25L).amount(new BigDecimal("20.00")).status(PaymentStatus.SUCCESS).build();

        when(paymentRepository.findByUserId(25L)).thenReturn(List.of(p1));

        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(25L);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo(25L);
    }

    @Test
    void getPaymentById_whenFound_returnsPaymentResponse() {
        Payment p = Payment.builder().id(15L).orderId(102L).userId(30L).amount(new BigDecimal("99.00")).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findById(15L)).thenReturn(Optional.of(p));

        PaymentResponse response = paymentService.getPaymentById(15L);
        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getAmount()).isEqualByComparingTo("99.00");
    }

    @Test
    void getPaymentById_whenNotFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found: 999");
    }
}
