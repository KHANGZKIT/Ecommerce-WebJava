package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(min = 10, max = 500, message = "Shipping address must be between 10 and 500 characters")
    @Pattern(regexp = "^[\\p{L}\\d\\s\\-'.&(),#]+$", message = "Shipping address contains invalid characters")
    @Schema(example = "123 Main Street, Tech Park, Hyderabad, Telangana, 500081")
    private String shippingAddress;

    @NotNull(message = "Payment method is required")
    @Schema(example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Pattern(regexp = "^[\\p{L}\\d\\s\\-'.&(),!?;:]*$", message = "Notes contain invalid characters")
    @Schema(example = "Please leave the package at the front door.")
    private String notes;
}
