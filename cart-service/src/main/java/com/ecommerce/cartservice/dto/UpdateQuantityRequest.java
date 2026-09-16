package com.ecommerce.cartservice.dto;

import com.ecommerce.cartservice.config.JacksonConfig;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class UpdateQuantityRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative (use 0 to remove)")
    @Max(value = CartService.MAX_QUANTITY, message = "Quantity cannot exceed " + CartService.MAX_QUANTITY)
    @JsonDeserialize(using = JacksonConfig.StrictIntegerDeserializer.class)
    @Schema(example = "5")
    private Integer quantity;
}
