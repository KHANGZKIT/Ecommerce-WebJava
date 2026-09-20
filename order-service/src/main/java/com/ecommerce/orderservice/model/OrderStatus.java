package com.ecommerce.orderservice.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_UNKNOWN,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
