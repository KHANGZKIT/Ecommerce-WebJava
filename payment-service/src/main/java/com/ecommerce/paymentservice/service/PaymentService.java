package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.client.MomoGatewayClient;
import com.ecommerce.paymentservice.client.OrderServiceClient;
import com.ecommerce.paymentservice.config.MomoProperties;
import com.ecommerce.paymentservice.dto.PaymentCallbackNotification;
import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.dto.momo.MomoCreatePaymentRequest;
import com.ecommerce.paymentservice.dto.momo.MomoCreatePaymentResponse;
import com.ecommerce.paymentservice.dto.momo.MomoIpnRequest;
import com.ecommerce.paymentservice.dto.momo.MomoQueryRequest;
import com.ecommerce.paymentservice.dto.momo.MomoQueryResponse;
import com.ecommerce.paymentservice.exception.ApiException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.exception.PaymentProcessingException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentMethod;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.momo.MomoCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MomoProperties momoProperties;
    private final MomoCryptoService momoCryptoService;
    private final MomoGatewayClient momoGatewayClient;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order {} amount {} method {}",
                request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        if (request.getPaymentMethod() == null) {
            throw new ApiException("Phương thức thanh toán không được để trống", HttpStatus.BAD_REQUEST);
        }

        // Branch to real MoMo gateway when method is MOMO
        if (request.getPaymentMethod() == PaymentMethod.MOMO) {
            return processMomoPayment(request);
        }

        // Reject unintegrated payment methods clearly (e.g. CREDIT_CARD, DEBIT_CARD, UPI, etc.)
        // Note: VNPAY is intended for website payment in future phases but is NOT yet implemented or verified.
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.FAILED)
                .message("Phương thức thanh toán " + request.getPaymentMethod()
                        + " hiện chưa được kích hoạt. TECHGEAR hiện chỉ hỗ trợ thanh toán qua Ví MoMo Sandbox. VNPAY chưa được triển khai và kiểm thử trong giai đoạn này.")
                .build();

        log.warn("Payment rejected for order {} with unsupported method {}. VNPAY not yet implemented.",
                request.getOrderId(), request.getPaymentMethod());

        return toDto(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public com.ecommerce.paymentservice.dto.PaymentStatisticsDto getAdminStatistics() {
        BigDecimal verifiedRevenue = paymentRepository.sumVerifiedRevenue();
        long successCount = paymentRepository.countVerifiedSuccessfulPayments();
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long totalCount = paymentRepository.count();

        return com.ecommerce.paymentservice.dto.PaymentStatisticsDto.builder()
                .totalVerifiedRevenue(verifiedRevenue != null ? verifiedRevenue : BigDecimal.ZERO)
                .verifiedSuccessCount(successCount)
                .failedCount(failedCount)
                .pendingCount(pendingCount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * Initiates a real one-time payment with MoMo Gateway via captureWallet API.
     */
    private PaymentResponse processMomoPayment(PaymentRequest request) {
        long momoAmount = request.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        if (momoAmount < 1000) {
            throw new ApiException("MoMo minimum transaction amount is 1,000 VND", HttpStatus.BAD_REQUEST);
        }

        // Generate unique identifiers
        String providerOrderId = "TG_ORD_" + request.getOrderId() + "_" + UUID.randomUUID().toString().substring(0, 8);
        String requestId = UUID.randomUUID().toString();
        String extraData = "orderId=" + request.getOrderId();
        String orderInfo = "TECHGEAR - Thanh toán đơn hàng #" + request.getOrderId();
        String redirectUrl = momoProperties.getRedirectUrl() + "?orderId=" + request.getOrderId();

        // Canonical signature string calculation
        String rawSignature = momoCryptoService.buildCreatePaymentRawSignature(
                momoProperties.getAccessKey(),
                momoAmount,
                extraData,
                momoProperties.getIpnUrl(),
                providerOrderId,
                orderInfo,
                momoProperties.getPartnerCode(),
                redirectUrl,
                requestId,
                momoProperties.getRequestType()
        );
        String signature = momoCryptoService.signHmacSha256(rawSignature, momoProperties.getSecretKey());

        // Create initial PENDING payment record
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.MOMO)
                .status(PaymentStatus.PENDING)
                .providerOrderId(providerOrderId)
                .requestId(requestId)
                .message("MoMo payment initiated, awaiting customer authorization")
                .build();

        payment = paymentRepository.save(payment);

        MomoCreatePaymentRequest momoRequest = MomoCreatePaymentRequest.builder()
                .partnerCode(momoProperties.getPartnerCode())
                .requestType(momoProperties.getRequestType())
                .ipnUrl(momoProperties.getIpnUrl())
                .redirectUrl(redirectUrl)
                .orderId(providerOrderId)
                .amount(momoAmount)
                .orderInfo(orderInfo)
                .requestId(requestId)
                .extraData(extraData)
                .autoCapture(true)
                .lang("vi")
                .signature(signature)
                .build();

        try {
            MomoCreatePaymentResponse momoResponse = momoGatewayClient.createPayment(momoRequest);
            if (momoResponse != null && Integer.valueOf(0).equals(momoResponse.getResultCode())
                    && momoResponse.getPayUrl() != null && !momoResponse.getPayUrl().isBlank()) {
                payment.setPayUrl(momoResponse.getPayUrl());
                payment.setQrCodeUrl(momoResponse.getQrCodeUrl());
                payment.setDeeplink(momoResponse.getDeeplink());
                payment.setMessage("MoMo checkout URL generated: " + momoResponse.getMessage());
                log.info("MoMo payment initiated successfully for order {}: payUrl={}",
                        request.getOrderId(), momoResponse.getPayUrl());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                String errMsg = momoResponse != null ? momoResponse.getMessage() : "Unknown MoMo error";
                payment.setMessage("MoMo initiation rejected: " + errMsg);
                log.warn("MoMo payment initiation rejected for order {}: {}", request.getOrderId(), errMsg);
            }
        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMessage("Failed to contact MoMo gateway: " + ex.getMessage());
            log.error("Exception calling MoMo gateway for order {}: {}", request.getOrderId(), ex.getMessage(), ex);
        }

        return toDto(paymentRepository.save(payment));
    }

    /**
     * Handles asynchronous Instant Payment Notification (IPN) webhook from MoMo.
     * Enforces signature verification, amount matching, idempotency, and updates Order Service.
     */
    @Transactional
    public void handleMomoIpn(MomoIpnRequest ipnRequest) {
        log.info("Received MoMo IPN callback for orderId: {}, transId: {}, resultCode: {}",
                ipnRequest.getOrderId(), ipnRequest.getTransId(), ipnRequest.getResultCode());

        // 1. Verify HMAC-SHA256 Signature
        String rawSignature = momoCryptoService.buildIpnRawSignature(
                momoProperties.getAccessKey(),
                ipnRequest.getAmount(),
                ipnRequest.getExtraData(),
                ipnRequest.getMessage(),
                ipnRequest.getOrderId(),
                ipnRequest.getOrderInfo(),
                ipnRequest.getOrderType(),
                ipnRequest.getPartnerCode(),
                ipnRequest.getPayType(),
                ipnRequest.getRequestId(),
                ipnRequest.getResponseTime() != null ? ipnRequest.getResponseTime() : 0L,
                ipnRequest.getResultCode(),
                ipnRequest.getTransId() != null ? ipnRequest.getTransId() : 0L
        );

        boolean signatureValid = momoCryptoService.verifySignature(
                rawSignature, momoProperties.getSecretKey(), ipnRequest.getSignature());

        if (!signatureValid) {
            log.warn("MoMo IPN signature verification failed for orderId: {}", ipnRequest.getOrderId());
            throw new ApiException("Invalid MoMo IPN signature", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate Partner Code
        if (!momoProperties.getPartnerCode().equals(ipnRequest.getPartnerCode())) {
            log.warn("MoMo IPN partner code mismatch: expected {}, received {}",
                    momoProperties.getPartnerCode(), ipnRequest.getPartnerCode());
            throw new ApiException("Partner code mismatch in IPN callback", HttpStatus.BAD_REQUEST);
        }

        // 3. Find Payment Entity by providerOrderId or requestId
        Payment payment = paymentRepository.findByProviderOrderId(ipnRequest.getOrderId())
                .or(() -> paymentRepository.findByRequestId(ipnRequest.getRequestId()))
                .orElseThrow(() -> {
                    log.error("Payment not found for MoMo IPN orderId: {}", ipnRequest.getOrderId());
                    return new PaymentNotFoundException("Payment record not found for MoMo orderId: " + ipnRequest.getOrderId());
                });

        // 4. Validate Amount
        long expectedAmount = payment.getAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        if (ipnRequest.getAmount() != expectedAmount) {
            log.error("Amount mismatch for orderId {}: expected {}, received {}",
                    ipnRequest.getOrderId(), expectedAmount, ipnRequest.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMessage("Amount mismatch detected: expected " + expectedAmount + ", received " + ipnRequest.getAmount());
            paymentRepository.save(payment);
            throw new ApiException("Amount mismatch in IPN callback", HttpStatus.BAD_REQUEST);
        }

        // 5. Idempotency Check: if already SUCCESS, acknowledge without re-executing side effects
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("MoMo IPN duplicate ignored for already SUCCESS payment {}", payment.getId());
            return;
        }

        // 6. Process Payment Result
        if (Integer.valueOf(0).equals(ipnRequest.getResultCode())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setMomoTransId(ipnRequest.getTransId());
            payment.setMessage("MoMo payment successful (Trans ID: " + ipnRequest.getTransId() + ")");
            paymentRepository.save(payment);
            log.info("MoMo payment SUCCESS for order {} txn {}", payment.getOrderId(), payment.getTransactionId());

            // Synchronize with Order Service: update order to CONFIRMED, clear cart, finalize stock
            orderServiceClient.notifyPaymentResult(payment.getOrderId(), PaymentCallbackNotification.builder()
                    .status("SUCCESS")
                    .transactionId(payment.getTransactionId())
                    .gatewayTransactionId(String.valueOf(ipnRequest.getTransId()))
                    .message("MoMo payment succeeded")
                    .build());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setMomoTransId(ipnRequest.getTransId());
            payment.setMessage("MoMo payment failed: " + ipnRequest.getMessage() + " (Code: " + ipnRequest.getResultCode() + ")");
            paymentRepository.save(payment);
            log.warn("MoMo payment FAILED for order {}: code={}, msg={}",
                    payment.getOrderId(), ipnRequest.getResultCode(), ipnRequest.getMessage());

            // Synchronize with Order Service: update order to PAYMENT_FAILED, release held stock
            orderServiceClient.notifyPaymentResult(payment.getOrderId(), PaymentCallbackNotification.builder()
                    .status("FAILED")
                    .transactionId(payment.getTransactionId())
                    .gatewayTransactionId(ipnRequest.getTransId() != null ? String.valueOf(ipnRequest.getTransId()) : null)
                    .message("MoMo payment failed: " + ipnRequest.getMessage())
                    .build());
        }
    }

    /**
     * Queries transaction status directly from MoMo API and synchronizes database state.
     */
    @Transactional
    public PaymentResponse queryMomoPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getPaymentMethod() != PaymentMethod.MOMO || payment.getProviderOrderId() == null) {
            return toDto(payment);
        }

        String requestId = UUID.randomUUID().toString();
        String rawSignature = momoCryptoService.buildQueryRawSignature(
                momoProperties.getAccessKey(),
                payment.getProviderOrderId(),
                momoProperties.getPartnerCode(),
                requestId
        );
        String signature = momoCryptoService.signHmacSha256(rawSignature, momoProperties.getSecretKey());

        MomoQueryRequest queryRequest = MomoQueryRequest.builder()
                .partnerCode(momoProperties.getPartnerCode())
                .orderId(payment.getProviderOrderId())
                .requestId(requestId)
                .lang("vi")
                .signature(signature)
                .build();

        try {
            MomoQueryResponse queryResponse = momoGatewayClient.queryPayment(queryRequest);
            if (queryResponse != null && Integer.valueOf(0).equals(queryResponse.getResultCode())) {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setMomoTransId(queryResponse.getTransId());
                    payment.setMessage("Synced from MoMo: " + queryResponse.getMessage());
                    payment = paymentRepository.save(payment);

                    orderServiceClient.notifyPaymentResult(payment.getOrderId(), PaymentCallbackNotification.builder()
                            .status("SUCCESS")
                            .transactionId(payment.getTransactionId())
                            .gatewayTransactionId(String.valueOf(queryResponse.getTransId()))
                            .message("MoMo payment confirmed via status query")
                            .build());
                }
            } else if (queryResponse != null && queryResponse.getResultCode() != null) {
                int code = queryResponse.getResultCode();
                // MoMo codes: 1000 = initiated / awaiting user payment, 7000 = processing, 9000 = authorized
                boolean isPending = (code == 1000 || code == 7000 || code == 9000);
                if (!isPending && payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setMessage("Synced from MoMo: " + queryResponse.getMessage() + " (Code: " + code + ")");
                    payment = paymentRepository.save(payment);

                    orderServiceClient.notifyPaymentResult(payment.getOrderId(), PaymentCallbackNotification.builder()
                            .status("FAILED")
                            .transactionId(payment.getTransactionId())
                            .message("MoMo payment failed: " + queryResponse.getMessage())
                            .build());
                }
            }
        } catch (Exception ex) {
            log.error("Error querying MoMo payment {}: {}", paymentId, ex.getMessage());
        }

        return toDto(payment);
    }

    /**
     * Synchronizes payment status for an order by querying MoMo directly if payment is in PENDING state.
     */
    @Transactional
    public PaymentResponse syncMomoPaymentByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException("No payment found for order: " + orderId);
        }
        Payment payment = payments.get(payments.size() - 1);
        if (payment.getPaymentMethod() == PaymentMethod.MOMO && payment.getStatus() == PaymentStatus.PENDING) {
            return queryMomoPayment(payment.getId());
        }
        return toDto(payment);
    }

    @Transactional
    public PaymentResponse processRefund(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + request.getPaymentId()));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException(
                    "Cannot refund payment in status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setMessage("Refunded. Reason: " + request.getReason());
        log.info("Refund processed for payment {}", payment.getId());
        return toDto(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        return toDto(paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    private PaymentResponse toDto(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .transactionId(p.getTransactionId())
                .orderId(p.getOrderId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .providerOrderId(p.getProviderOrderId())
                .payUrl(p.getPayUrl())
                .qrCodeUrl(p.getQrCodeUrl())
                .deeplink(p.getDeeplink())
                .momoTransId(p.getMomoTransId())
                .message(p.getMessage())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
