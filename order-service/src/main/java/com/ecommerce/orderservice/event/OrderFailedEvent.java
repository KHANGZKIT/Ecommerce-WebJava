package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event fired when an order's payment has FAILED.
 * Consumed by notification-service to send failure email.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderFailedEvent implements Serializable {
    private Long orderId;
    private Long userId;
    private String reason;
}
