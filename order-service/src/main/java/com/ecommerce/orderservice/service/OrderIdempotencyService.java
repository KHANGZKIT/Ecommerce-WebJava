package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.model.CartClearStatus;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.StockReleaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdempotencyService {

    private static final ObjectMapper CANONICAL_MAPPER = new ObjectMapper();

    @JsonPropertyOrder({"shippingAddress", "paymentMethod", "notes"})
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record CanonicalCheckoutPayload(
            String shippingAddress,
            String paymentMethod,
            String notes
    ) {}

    private final OrderIdempotencyPersistenceService persistenceService;

    public OrderIdempotency getOrCreate(Long userId, String idempotencyKey, CheckoutRequest request) {
        validateKey(idempotencyKey);
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Checkout request cannot be null");
        }

        String fingerprint = computeFingerprint(request);

        // 1. Fast-path lookup (read-only)
        Optional<OrderIdempotency> existing = persistenceService.find(userId, idempotencyKey);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), fingerprint);
        }

        // 2. Attempt INSERT in a dedicated transaction
        try {
            return persistenceService.create(userId, idempotencyKey, fingerprint);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate-key insert detected for user {} and key {}: {}",
                    userId, idempotencyKey, ex.getMessage());
            // 3. Recovery: failed transaction has ended. Perform fresh read in a clean read transaction.
            return persistenceService.find(userId, idempotencyKey)
                    .map(record -> replayOrConflict(record, fingerprint))
                    .orElseThrow(() -> ex);
        }
    }

    public OrderIdempotency replayOrConflict(OrderIdempotency record, String requestFingerprint) {
        if (!record.getRequestFingerprint().equals(requestFingerprint)) {
            throw new ApiException(
                    "Idempotency key conflict: payload does not match existing request for key: " + record.getIdempotencyKey(),
                    HttpStatus.CONFLICT);
        }
        log.info("Replaying existing idempotency record for user {} and key {}",
                record.getUserId(), record.getIdempotencyKey());
        return record;
    }

    public static String toCanonicalJson(CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null for fingerprinting");
        }
        CanonicalCheckoutPayload payload = new CanonicalCheckoutPayload(
                request.getShippingAddress(),
                request.getPaymentMethod() != null ? request.getPaymentMethod().name() : null,
                request.getNotes()
        );
        try {
            return CANONICAL_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize canonical checkout payload", e);
        }
    }

    public static String computeFingerprint(CheckoutRequest request) {
        String canonicalJson = toCanonicalJson(request);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public void validateKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required and must not be blank");
        }
        if (idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key header must not exceed 128 characters");
        }
    }

    public OrderIdempotency attachOrder(Long recordId, Order order) {
        return persistenceService.attachOrder(recordId, order);
    }

    public Optional<OrderIdempotency> find(Long userId, String idempotencyKey) {
        return persistenceService.find(userId, idempotencyKey);
    }

    public OrderIdempotency updateRecoveryMetadata(
            Long recordId,
            String stockReservationReference,
            LocalDateTime paymentAttemptStartedAt,
            StockReleaseStatus stockReleaseStatus,
            CartClearStatus cartClearStatus) {
        return persistenceService.updateRecoveryMetadata(
                recordId, stockReservationReference, paymentAttemptStartedAt, stockReleaseStatus, cartClearStatus);
    }

    public Order persistPendingOrderAndLink(
            Order order,
            java.util.List<com.ecommerce.orderservice.model.OrderItem> items,
            Long recordId,
            String stockReservationReference) {
        return persistenceService.persistPendingOrderAndLink(order, items, recordId, stockReservationReference);
    }

    public boolean claimPaymentAttempt(Long recordId) {
        if (recordId == null) {
            throw new IllegalArgumentException("Record ID is required to claim payment attempt");
        }
        return persistenceService.claimPaymentAttempt(recordId);
    }
}
