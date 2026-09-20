package com.ecommerce.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockResponse {

    @Schema(example = "RELEASED")
    private String status;

    @Schema(example = "2")
    private int releasedItemCount;
}
