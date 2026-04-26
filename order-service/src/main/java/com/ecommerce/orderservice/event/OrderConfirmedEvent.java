package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event fired when an order has been CONFIRMED (payment succeeded).
 * Consumed by notification-service to send confirmation email.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderConfirmedEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private String transactionId;
    private BigDecimal totalAmount;
}
