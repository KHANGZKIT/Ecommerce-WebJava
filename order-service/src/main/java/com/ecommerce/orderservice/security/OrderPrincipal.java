package com.ecommerce.orderservice.security;

/** Identity from a signature-verified JWT; never populated from identity headers. */
public record OrderPrincipal(long userId, String subject) {}
