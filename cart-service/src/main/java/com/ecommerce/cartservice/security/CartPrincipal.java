package com.ecommerce.cartservice.security;

/** Identity from a signature-verified JWT; never populated from identity headers. */
public record CartPrincipal(long userId, String subject) {}
