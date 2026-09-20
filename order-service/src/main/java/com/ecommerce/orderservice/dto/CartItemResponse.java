package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Client DTO representing an item in the Cart Service API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;

    public CartItemResponse(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
