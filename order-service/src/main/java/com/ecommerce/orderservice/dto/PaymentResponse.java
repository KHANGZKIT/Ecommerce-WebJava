package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Client DTO for payment-service response */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    private Long id;
    private String transactionId;
    private Long orderId;
    private String status;   // SUCCESS | FAILED | UNKNOWN | REFUNDED
    private String message;

    public boolean isSuccess() {
        return STATUS_SUCCESS.equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equalsIgnoreCase(status);
    }

    public boolean isUnknown() {
        return STATUS_UNKNOWN.equalsIgnoreCase(status);
    }
}

