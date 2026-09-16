package com.ecommerce.productservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryUpdateRequest {

    @NotNull(message = "Quantity is required")
    // DELTA is signed; ABSOLUTE must be nonnegative.
    private Integer quantity;

    /** ABSOLUTE sets exact stock; DELTA adds/subtracts */
    public enum UpdateMode { ABSOLUTE, DELTA }

    @NotNull(message = "Update mode is required")
    private UpdateMode mode;

    @JsonIgnore
    @AssertTrue(message = "Quantity cannot be negative in ABSOLUTE mode")
    public boolean isQuantityValidForMode() {
        // Missing fields are reported by their @NotNull constraints.
        return quantity == null || mode == null || mode == UpdateMode.DELTA || quantity >= 0;
    }
}
