package com.ecommerce.productservice.exception;

import org.springframework.http.HttpStatus;

public class CategoryInUseException extends ApiException {
    public CategoryInUseException(String msg) { super(msg, HttpStatus.CONFLICT); }
}
