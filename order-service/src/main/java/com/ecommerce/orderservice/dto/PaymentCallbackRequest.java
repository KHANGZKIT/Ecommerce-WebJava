package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {

    @NotBlank(message = "Payment status is required")
    private String status;               // "SUCCESS" or "FAILED"

    private String transactionId;        // TECHGEAR internal transaction ID
    private String gatewayTransactionId; // MoMo transId
    private String message;
}
