package com.ecommerce.paymentservice.service.momo;

import com.ecommerce.paymentservice.client.MomoGatewayClient;
import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.config.MomoProperties;
import com.ecommerce.paymentservice.dto.PaymentCallbackNotification;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.momo.MomoCreatePaymentResponse;
import com.ecommerce.paymentservice.dto.momo.MomoIpnRequest;
import com.ecommerce.paymentservice.exception.ApiException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MomoPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MomoGatewayClient momoGatewayClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    private MomoProperties momoProperties;
    private MomoCryptoService momoCryptoService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        momoProperties = new MomoProperties();
        momoProperties.setPartnerCode("MOMO");
        momoProperties.setAccessKey("F8BBA842ECF85");
        momoProperties.setSecretKey("K951B6PE1waDMi640xX0qPDp5Jm0nhBR");
        momoProperties.setEndpoint("https://test-payment.momo.vn");
        momoProperties.setRedirectUrl("http://localhost:3000/payment-result");
        momoProperties.setIpnUrl("http://localhost:8080/api/v1/payments/momo/ipn");

        momoCryptoService = new MomoCryptoService();

        paymentService = new PaymentService(
                paymentRepository,
                momoProperties,
                momoCryptoService,
                momoGatewayClient,
                orderServiceClient
        );
    }

    @Test
    @DisplayName("Should successfully initiate MoMo payment and return PENDING status with payUrl")
    void shouldInitiateMomoPaymentSuccessfully() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(101L);
        request.setUserId(1L);
        request.setAmount(new BigDecimal("15000000"));
        request.setPaymentMethod(PaymentMethod.MOMO);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        MomoCreatePaymentResponse momoResponse = MomoCreatePaymentResponse.builder()
                .partnerCode("MOMO")
                .orderId("TG_ORD_101_abc")
                .resultCode(0)
                .message("Success")
                .payUrl("https://test-payment.momo.vn/v2/gateway/pay?s=testSessionId")
                .qrCodeUrl("https://test-payment.momo.vn/qr/test")
                .deeplink("momo://payment?test")
                .build();

        when(momoGatewayClient.createPayment(any())).thenReturn(momoResponse);

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.MOMO);
        assertThat(response.getPayUrl()).isEqualTo("https://test-payment.momo.vn/v2/gateway/pay?s=testSessionId");
        assertThat(response.getQrCodeUrl()).isEqualTo("https://test-payment.momo.vn/qr/test");
        assertThat(response.getDeeplink()).isEqualTo("momo://payment?test");

        verify(paymentRepository, atLeast(2)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject MoMo payment when amount is under 1,000 VND")
    void shouldRejectWhenAmountUnder1000Vnd() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(101L);
        request.setUserId(1L);
        request.setAmount(new BigDecimal("500"));
        request.setPaymentMethod(PaymentMethod.MOMO);

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("minimum transaction amount is 1,000 VND");
    }

    @Test
    @DisplayName("Should handle successful IPN callback and notify Order Service")
    void shouldHandleSuccessfulIpnCallback() {
        String providerOrderId = "TG_ORD_101_xyz";
        String requestId = "req-uuid-123";
        long amount = 15000000L;

        Payment existingPayment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("15000000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findByProviderOrderId(providerOrderId))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Build valid IPN with signature
        String rawSig = momoCryptoService.buildIpnRawSignature(
                momoProperties.getAccessKey(),
                amount,
                "orderId=101",
                "Success",
                providerOrderId,
                "TECHGEAR - Thanh toán đơn hàng #101",
                "momo_wallet",
                "MOMO",
                "qr",
                requestId,
                1713456789000L,
                0,
                99887766L
        );
        String signature = momoCryptoService.signHmacSha256(rawSig, momoProperties.getSecretKey());

        MomoIpnRequest ipnRequest = MomoIpnRequest.builder()
                .partnerCode("MOMO")
                .orderId(providerOrderId)
                .requestId(requestId)
                .amount(amount)
                .orderInfo("TECHGEAR - Thanh toán đơn hàng #101")
                .orderType("momo_wallet")
                .transId(99887766L)
                .resultCode(0)
                .message("Success")
                .payType("qr")
                .responseTime(1713456789000L)
                .extraData("orderId=101")
                .signature(signature)
                .build();

        paymentService.handleMomoIpn(ipnRequest);

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(existingPayment.getMomoTransId()).isEqualTo(99887766L);

        ArgumentCaptor<PaymentCallbackNotification> captor = ArgumentCaptor.forClass(PaymentCallbackNotification.class);
        verify(orderServiceClient).notifyPaymentResult(eq(101L), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getTransactionId()).isEqualTo("TECHGEAR_TXN_001");
        assertThat(captor.getValue().getGatewayTransactionId()).isEqualTo("99887766");
    }

    @Test
    @DisplayName("Should reject IPN callback when signature is invalid")
    void shouldRejectIpnWhenSignatureInvalid() {
        MomoIpnRequest ipnRequest = MomoIpnRequest.builder()
                .partnerCode("MOMO")
                .orderId("TG_ORD_101_xyz")
                .requestId("req-1")
                .amount(15000000L)
                .resultCode(0)
                .message("Success")
                .signature("invalid-forged-signature-hex")
                .build();

        assertThatThrownBy(() -> paymentService.handleMomoIpn(ipnRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid MoMo IPN signature");

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(orderServiceClient);
    }

    @Test
    @DisplayName("Should reject IPN callback when amount does not match stored payment amount")
    void shouldRejectIpnWhenAmountMismatched() {
        String providerOrderId = "TG_ORD_101_xyz";
        String requestId = "req-uuid-123";

        Payment existingPayment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("20000000")) // Stored amount is 20,000,000
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .build();

        when(paymentRepository.findByProviderOrderId(providerOrderId))
                .thenReturn(Optional.of(existingPayment));

        // IPN specifies 15,000,000
        long tamperedAmount = 15000000L;
        String rawSig = momoCryptoService.buildIpnRawSignature(
                momoProperties.getAccessKey(),
                tamperedAmount,
                "",
                "Success",
                providerOrderId,
                "Info",
                "momo_wallet",
                "MOMO",
                "qr",
                requestId,
                1000L,
                0,
                99887766L
        );
        String signature = momoCryptoService.signHmacSha256(rawSig, momoProperties.getSecretKey());

        MomoIpnRequest ipnRequest = MomoIpnRequest.builder()
                .partnerCode("MOMO")
                .orderId(providerOrderId)
                .requestId(requestId)
                .amount(tamperedAmount)
                .orderInfo("Info")
                .orderType("momo_wallet")
                .transId(99887766L)
                .resultCode(0)
                .message("Success")
                .payType("qr")
                .responseTime(1000L)
                .extraData("")
                .signature(signature)
                .build();

        assertThatThrownBy(() -> paymentService.handleMomoIpn(ipnRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Amount mismatch");

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(existingPayment);
        verifyNoInteractions(orderServiceClient);
    }

    @Test
    @DisplayName("Should be idempotent when duplicate IPN arrives for already SUCCESS payment")
    void shouldHandleDuplicateIpnIdempotently() {
        String providerOrderId = "TG_ORD_101_xyz";
        String requestId = "req-uuid-123";
        long amount = 15000000L;

        Payment existingPayment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("15000000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.SUCCESS) // Already SUCCESS
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .build();

        when(paymentRepository.findByProviderOrderId(providerOrderId))
                .thenReturn(Optional.of(existingPayment));

        String rawSig = momoCryptoService.buildIpnRawSignature(
                momoProperties.getAccessKey(),
                amount,
                "",
                "Success",
                providerOrderId,
                "Info",
                "momo_wallet",
                "MOMO",
                "qr",
                requestId,
                1000L,
                0,
                99887766L
        );
        String signature = momoCryptoService.signHmacSha256(rawSig, momoProperties.getSecretKey());

        MomoIpnRequest ipnRequest = MomoIpnRequest.builder()
                .partnerCode("MOMO")
                .orderId(providerOrderId)
                .requestId(requestId)
                .amount(amount)
                .orderInfo("Info")
                .orderType("momo_wallet")
                .transId(99887766L)
                .resultCode(0)
                .message("Success")
                .payType("qr")
                .responseTime(1000L)
                .extraData("")
                .signature(signature)
                .build();

        // Should return gracefully without throwing or calling orderServiceClient
        paymentService.handleMomoIpn(ipnRequest);

        verify(orderServiceClient, never()).notifyPaymentResult(anyLong(), any());
    }

    @Test
    @DisplayName("Should handle failed IPN callback (resultCode != 0) and notify Order Service of failure")
    void shouldHandleFailedIpnCallback() {
        String providerOrderId = "TG_ORD_101_xyz";
        String requestId = "req-uuid-123";
        long amount = 15000000L;

        Payment existingPayment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("15000000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findByProviderOrderId(providerOrderId))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // resultCode 1006: User cancelled payment
        String rawSig = momoCryptoService.buildIpnRawSignature(
                momoProperties.getAccessKey(),
                amount,
                "",
                "User cancelled transaction",
                providerOrderId,
                "Info",
                "momo_wallet",
                "MOMO",
                "qr",
                requestId,
                1000L,
                1006,
                99887766L
        );
        String signature = momoCryptoService.signHmacSha256(rawSig, momoProperties.getSecretKey());

        MomoIpnRequest ipnRequest = MomoIpnRequest.builder()
                .partnerCode("MOMO")
                .orderId(providerOrderId)
                .requestId(requestId)
                .amount(amount)
                .orderInfo("Info")
                .orderType("momo_wallet")
                .transId(99887766L)
                .resultCode(1006)
                .message("User cancelled transaction")
                .payType("qr")
                .responseTime(1000L)
                .extraData("")
                .signature(signature)
                .build();

        paymentService.handleMomoIpn(ipnRequest);

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(existingPayment.getMessage()).contains("1006");

        ArgumentCaptor<PaymentCallbackNotification> captor = ArgumentCaptor.forClass(PaymentCallbackNotification.class);
        verify(orderServiceClient).notifyPaymentResult(eq(101L), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Should successfully query MoMo transaction and confirm payment when resultCode is 0")
    void shouldSuccessfullyQueryAndConfirmPaymentWhenMomoReturnsZero() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId("TG_ORD_101_abc")
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.ecommerce.paymentservice.dto.momo.MomoQueryResponse queryResponse =
                com.ecommerce.paymentservice.dto.momo.MomoQueryResponse.builder()
                        .partnerCode("MOMO")
                        .orderId("TG_ORD_101_abc")
                        .resultCode(0)
                        .message("Thành công.")
                        .transId(9988776655L)
                        .build();

        when(momoGatewayClient.queryPayment(any())).thenReturn(queryResponse);

        PaymentResponse response = paymentService.queryMomoPayment(1L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getMomoTransId()).isEqualTo(9988776655L);
        verify(orderServiceClient).notifyPaymentResult(eq(101L), any());
    }

    @Test
    @DisplayName("Should keep payment PENDING when MoMo query returns resultCode 1000 (awaiting user confirmation)")
    void shouldRemainPendingWhenMomoQueryReturns1000() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId("TG_ORD_101_abc")
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        com.ecommerce.paymentservice.dto.momo.MomoQueryResponse queryResponse =
                com.ecommerce.paymentservice.dto.momo.MomoQueryResponse.builder()
                        .partnerCode("MOMO")
                        .orderId("TG_ORD_101_abc")
                        .resultCode(1000)
                        .message("Giao dịch đã được khởi tạo, chờ người dùng xác nhận.")
                        .build();

        when(momoGatewayClient.queryPayment(any())).thenReturn(queryResponse);

        PaymentResponse response = paymentService.queryMomoPayment(1L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orderServiceClient, never()).notifyPaymentResult(anyLong(), any());
    }

    @Test
    @DisplayName("Should mark payment FAILED when MoMo query returns user cancelled resultCode 1006")
    void shouldMarkFailedWhenMomoQueryReturnsCancelled() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId("TG_ORD_101_abc")
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.ecommerce.paymentservice.dto.momo.MomoQueryResponse queryResponse =
                com.ecommerce.paymentservice.dto.momo.MomoQueryResponse.builder()
                        .partnerCode("MOMO")
                        .orderId("TG_ORD_101_abc")
                        .resultCode(1006)
                        .message("Người dùng hủy giao dịch.")
                        .build();

        when(momoGatewayClient.queryPayment(any())).thenReturn(queryResponse);

        PaymentResponse response = paymentService.queryMomoPayment(1L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderServiceClient).notifyPaymentResult(eq(101L), any());
    }

    @Test
    @DisplayName("Should sync MoMo payment by orderId")
    void shouldSyncMomoPaymentByOrderId() {
        Payment payment = Payment.builder()
                .id(1L)
                .orderId(101L)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId("TG_ORD_101_abc")
                .transactionId("TECHGEAR_TXN_001")
                .build();

        when(paymentRepository.findByOrderId(101L)).thenReturn(java.util.List.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.ecommerce.paymentservice.dto.momo.MomoQueryResponse queryResponse =
                com.ecommerce.paymentservice.dto.momo.MomoQueryResponse.builder()
                        .partnerCode("MOMO")
                        .orderId("TG_ORD_101_abc")
                        .resultCode(0)
                        .message("Thành công.")
                        .transId(12345678L)
                        .build();

        when(momoGatewayClient.queryPayment(any())).thenReturn(queryResponse);

        PaymentResponse response = paymentService.syncMomoPaymentByOrderId(101L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
