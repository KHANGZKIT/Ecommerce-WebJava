package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.CheckoutRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutClientSecurityContractTest {

    @Test
    void checkoutRequest_mustNotGainClientControllableIdentityOrPricingFields() {
        Set<String> fieldNames = Arrays.stream(CheckoutRequest.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        // Architectural rule: CheckoutRequest must not contain user identity, item quantities, or amounts
        assertThat(fieldNames).doesNotContain(
                "userId",
                "productId",
                "quantity",
                "price",
                "total",
                "totalAmount",
                "stock",
                "paymentAmount",
                "paymentStatus",
                "transactionId",
                "orderStatus"
        );
    }
}
