package com.ecommerce.paymentservice;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.PaymentStatisticsDto;
import com.ecommerce.paymentservice.exception.ApiException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentConsistencyRegressionTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    /**
     * REG-TEST-01: Non-integrated payment methods (e.g. CREDIT_CARD) must be rejected
     * without mock success. Must not forge SUCCESS status.
     */
    @Test
    @DisplayName("REG-TEST-01: Submit checkout with paymentMethod = CREDIT_CARD. System rejects without mock success.")
    void testNonIntegratedPaymentMethodRejection() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(19L);
        request.setUserId(33L);
        request.setAmount(new BigDecimal("8990000.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(19L);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getMessage()).contains("chưa được kích hoạt");
        assertThat(response.getMessage()).contains("VNPAY chưa được triển khai");
        assertThat(response.getMomoTransId()).isNull();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    /**
     * REG-TEST-04: Admin revenue calculation must count only verified transactions
     * (status = SUCCESS and momoTransId IS NOT NULL).
     * Unverified mock payments (like Order #19) and failed payments are excluded.
     */
    @Test
    @DisplayName("REG-TEST-04: Verified revenue calculation excludes unverified mock and failed transactions.")
    void testRevenueExcludesUnverifiedOrFailedPayments() {
        // Scenario:
        // 1 verified MoMo order: 100,000 VND (SUCCESS, momoTransId = 998877L)
        // 1 failed order: 200,000 VND (FAILED)
        // 1 unverified mock order: 300,000 VND (SUCCESS, momoTransId = null -> e.g. legacy Order #19)
        // Authoritative query sumVerifiedRevenue() sums ONLY verified records
        when(paymentRepository.sumVerifiedRevenue()).thenReturn(new BigDecimal("100000.00"));
        when(paymentRepository.countVerifiedSuccessfulPayments()).thenReturn(1L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(1L);
        when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(0L);
        when(paymentRepository.count()).thenReturn(3L);

        PaymentStatisticsDto stats = paymentService.getAdminStatistics();

        assertThat(stats.getTotalVerifiedRevenue()).isEqualByComparingTo("100000.00");
        assertThat(stats.getVerifiedSuccessCount()).isEqualTo(1L);
        assertThat(stats.getFailedCount()).isEqualTo(1L);
        assertThat(stats.getTotalCount()).isEqualTo(3L);

        verify(paymentRepository).sumVerifiedRevenue();
    }

    @Test
    @DisplayName("Reject payment with null paymentMethod")
    void testNullPaymentMethodRejection() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(100L);
        request.setUserId(1L);
        request.setAmount(new BigDecimal("50000"));
        request.setPaymentMethod(null);

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("không được để trống");
    }
}
