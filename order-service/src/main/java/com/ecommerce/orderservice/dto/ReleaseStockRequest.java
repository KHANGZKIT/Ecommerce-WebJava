package com.ecommerce.orderservice.dto;

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
    private String checkoutOperationKey;

    @Valid
    private List<StockItemRequest> items;

    public ReleaseStockRequest(String checkoutOperationKey) {
        this.checkoutOperationKey = checkoutOperationKey;
    }
}
