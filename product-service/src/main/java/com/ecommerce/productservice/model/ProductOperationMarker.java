package com.ecommerce.productservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_operation_markers", indexes = {
        @Index(name = "idx_operation_marker_key", columnList = "checkout_operation_key", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOperationMarker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_operation_key", nullable = false, unique = true, length = 128)
    private String checkoutOperationKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductOperationStatus status;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_operation_marker_items", joinColumns = @JoinColumn(name = "marker_id"))
    @Builder.Default
    private List<ProductOperationItem> items = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
