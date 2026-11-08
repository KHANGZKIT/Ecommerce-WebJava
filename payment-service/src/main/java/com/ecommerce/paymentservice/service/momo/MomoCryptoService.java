package com.ecommerce.paymentservice.service.momo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MoMo Cryptographic & Signature service implementing HMAC-SHA256 canonical hashing
 * according to official MoMo Payment Gateway v3 specification.
 */
@Slf4j
@Service
public class MomoCryptoService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Compute HMAC-SHA256 hex digest of data using the secret key.
     *
     * @param data Raw canonical string to sign
     * @param key  Secret key
     * @return Lowercase hex-encoded HMAC-SHA256 digest
     */
    public String signHmacSha256(String data, String key) {
        if (data == null || key == null) {
            throw new IllegalArgumentException("Data and key must not be null for HMAC-SHA256 calculation");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Failed to calculate HMAC-SHA256 signature: {}", ex.getMessage());
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", ex);
        }
    }

    /**
     * Build canonical raw signature string for create payment request (alphabetical sorting).
     * Format:
     * accessKey=$accessKey&amount=$amount&extraData=$extraData&ipnUrl=$ipnUrl&orderId=$orderId&orderInfo=$orderInfo&partnerCode=$partnerCode&redirectUrl=$redirectUrl&requestId=$requestId&requestType=$requestType
     */
    public String buildCreatePaymentRawSignature(
            String accessKey,
            long amount,
            String extraData,
            String ipnUrl,
            String orderId,
            String orderInfo,
            String partnerCode,
            String redirectUrl,
            String requestId,
            String requestType) {

        return "accessKey=" + (accessKey != null ? accessKey : "") +
                "&amount=" + amount +
                "&extraData=" + (extraData != null ? extraData : "") +
                "&ipnUrl=" + (ipnUrl != null ? ipnUrl : "") +
                "&orderId=" + (orderId != null ? orderId : "") +
                "&orderInfo=" + (orderInfo != null ? orderInfo : "") +
                "&partnerCode=" + (partnerCode != null ? partnerCode : "") +
                "&redirectUrl=" + (redirectUrl != null ? redirectUrl : "") +
                "&requestId=" + (requestId != null ? requestId : "") +
                "&requestType=" + (requestType != null ? requestType : "");
    }

    /**
     * Build canonical raw signature string for IPN callback verification (alphabetical sorting).
     * Format:
     * accessKey=$accessKey&amount=$amount&extraData=$extraData&message=$message&orderId=$orderId&orderInfo=$orderInfo&orderType=$orderType&partnerCode=$partnerCode&payType=$payType&requestId=$requestId&responseTime=$responseTime&resultCode=$resultCode&transId=$transId
     */
    public String buildIpnRawSignature(
            String accessKey,
            long amount,
            String extraData,
            String message,
            String orderId,
            String orderInfo,
            String orderType,
            String partnerCode,
            String payType,
            String requestId,
            long responseTime,
            int resultCode,
            long transId) {

        return "accessKey=" + (accessKey != null ? accessKey : "") +
                "&amount=" + amount +
                "&extraData=" + (extraData != null ? extraData : "") +
                "&message=" + (message != null ? message : "") +
                "&orderId=" + (orderId != null ? orderId : "") +
                "&orderInfo=" + (orderInfo != null ? orderInfo : "") +
                "&orderType=" + (orderType != null ? orderType : "") +
                "&partnerCode=" + (partnerCode != null ? partnerCode : "") +
                "&payType=" + (payType != null ? payType : "") +
                "&requestId=" + (requestId != null ? requestId : "") +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId;
    }

    /**
     * Build canonical raw signature string for query transaction status.
     * Format:
     * accessKey=$accessKey&orderId=$orderId&partnerCode=$partnerCode&requestId=$requestId
     */
    public String buildQueryRawSignature(String accessKey, String orderId, String partnerCode, String requestId) {
        return "accessKey=" + (accessKey != null ? accessKey : "") +
                "&orderId=" + (orderId != null ? orderId : "") +
                "&partnerCode=" + (partnerCode != null ? partnerCode : "") +
                "&requestId=" + (requestId != null ? requestId : "");
    }

    /**
     * Constant-time signature verification preventing timing attack vulnerabilities.
     */
    public boolean verifySignature(String rawData, String secretKey, String expectedSignature) {
        if (rawData == null || secretKey == null || expectedSignature == null) {
            return false;
        }
        String calculated = signHmacSha256(rawData, secretKey);
        return MessageDigest.isEqual(
                calculated.getBytes(StandardCharsets.UTF_8),
                expectedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}
