package com.ecommerce.orderservice.exception;

import org.springframework.http.HttpStatus;

public class CartNotFoundException extends ApiException {
    public CartNotFoundException(String msg) {
        super(msg, HttpStatus.NOT_FOUND);
    }
}
