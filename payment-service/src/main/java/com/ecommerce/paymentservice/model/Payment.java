package com.ecommerce.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "orderId"),
        @Index(name = "idx_payments_transaction_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_payments_provider_order_id", columnList = "providerOrderId"),
        @Index(name = "idx_payments_request_id", columnList = "requestId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    @Builder.Default
    private String transactionId = UUID.randomUUID().toString();

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Unique order identifier sent to external payment gateway (e.g. MoMo) */
    @Column(length = 100)
    private String providerOrderId;

    /** Unique request identifier sent to external payment gateway */
    @Column(length = 100)
    private String requestId;

    /** MoMo transaction identifier returned in IPN callback */
    private Long momoTransId;

    /** Gateway checkout payment URL for customer redirection */
    @Column(columnDefinition = "TEXT")
    private String payUrl;

    /** Gateway QR Code URL for mobile scanning */
    @Column(columnDefinition = "TEXT")
    private String qrCodeUrl;

    /** Gateway app deep link */
    @Column(columnDefinition = "TEXT")
    private String deeplink;

    /** Human-readable reason for failure or info message */
    @Column(columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
