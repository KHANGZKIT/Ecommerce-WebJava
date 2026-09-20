package com.ecommerce.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldStockResponse {

    @Schema(description = "List of held items with authoritative product snapshot data")
    private List<HeldItemDto> items;
}
