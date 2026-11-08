package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackNotification {
    private String status;               // "SUCCESS" or "FAILED"
    private String transactionId;        // TECHGEAR internal transaction ID
    private String gatewayTransactionId; // MoMo transId
    private String message;
}
