package com.ecommerce.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldStockRequest {

    @NotBlank(message = "Checkout operation key is required")
    @Size(max = 128, message = "Checkout operation key must not exceed 128 characters")
    @Schema(description = "Unique checkout operation key for idempotency", example = "chk-op-12345")
    private String checkoutOperationKey;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    @Schema(description = "List of items to hold stock for")
    private List<StockItemRequest> items;
}
