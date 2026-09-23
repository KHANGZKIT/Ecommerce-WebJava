package com.ecommerce.paymentservice.security;

public record PaymentPrincipal(long userId, String subject, String role) {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
