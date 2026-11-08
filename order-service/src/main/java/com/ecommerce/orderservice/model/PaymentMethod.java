package com.ecommerce.orderservice.model;

/** Local copy of PaymentMethod - mirrors payment-service enum to avoid cross-service dependency */
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    WALLET,
    MOMO
}
