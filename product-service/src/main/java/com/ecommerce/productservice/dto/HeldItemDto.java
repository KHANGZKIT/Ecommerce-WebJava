package com.ecommerce.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeldItemDto {

    @Schema(example = "1")
    private Long productId;

    @Schema(example = "Wireless Mouse")
    private String name;

    @Schema(example = "MOU-001")
    private String sku;

    @Schema(example = "25.00")
    private BigDecimal price;

    @Schema(example = "2")
    private Integer quantity;
}
