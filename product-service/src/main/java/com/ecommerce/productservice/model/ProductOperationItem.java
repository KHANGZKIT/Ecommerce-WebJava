package com.ecommerce.productservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOperationItem {

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 100)
    private String productSku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;
}
