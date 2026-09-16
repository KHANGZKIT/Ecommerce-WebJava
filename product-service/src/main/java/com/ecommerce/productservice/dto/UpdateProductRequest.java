package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\d\\s\\-'.&()]+$", message = "Product name contains invalid characters")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Pattern(regexp = "^[\\p{L}\\d\\s\\-'.&(),!?;:/\\n\\r]*$", message = "Description contains invalid characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private String imageUrl;

    private Long categoryId;

    private Boolean active;
}
