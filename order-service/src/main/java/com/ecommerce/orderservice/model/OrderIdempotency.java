package com.ecommerce.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "order_idempotency",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_order_idempotency_user_key",
            columnNames = {"user_id", "idempotency_key"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @Setter
    private Order order;

    @Column(name = "stock_reservation_reference", length = 128)
    @Setter
    private String stockReservationReference;

    @Column(name = "payment_attempt_started_at")
    @Setter
    private LocalDateTime paymentAttemptStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_release_status", nullable = false, length = 32)
    @Setter
    @Builder.Default
    private StockReleaseStatus stockReleaseStatus = StockReleaseStatus.NOT_REQUIRED;

    @Enumerated(EnumType.STRING)
    @Column(name = "cart_clear_status", nullable = false, length = 32)
    @Setter
    @Builder.Default
    private CartClearStatus cartClearStatus = CartClearStatus.NOT_REQUIRED;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Setter
    private LocalDateTime updatedAt;

    public void attachOrder(Order order) {
        this.order = order;
        this.updatedAt = LocalDateTime.now();
    }
}
