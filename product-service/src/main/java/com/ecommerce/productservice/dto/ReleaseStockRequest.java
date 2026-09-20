package com.ecommerce.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class ReleaseStockRequest {

    @NotBlank(message = "Checkout operation key is required")
    @Size(max = 128, message = "Checkout operation key must not exceed 128 characters")
    @Schema(description = "Unique checkout operation key for idempotency", example = "chk-op-12345")
    private String checkoutOperationKey;

    @Valid
    @Schema(description = "Optional items list; the durable marker is authoritative for released quantities")
    private List<StockItemRequest> items;

    public ReleaseStockRequest(String checkoutOperationKey) {
        this.checkoutOperationKey = checkoutOperationKey;
    }
}
