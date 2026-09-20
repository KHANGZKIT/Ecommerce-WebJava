package com.ecommerce.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;

@Component
public class OrderJwtVerifier {
    private final JwtParser parser;

    public OrderJwtVerifier(@Value("${jwt.secret}") String secret) {
        try {
            parser = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                    .build();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT secret must be valid Base64 encoding at least 32 bytes");
        }
    }

    public OrderPrincipal verify(String token) {
        Jws<Claims> jwt = parser.parseClaimsJws(token);
        Claims claims = jwt.getBody();
        Object id = claims.get("userId");
        if (!"HS256".equals(jwt.getHeader().getAlgorithm())
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null || !claims.getExpiration().after(new Date())
                || !(id instanceof Integer || id instanceof Long)
                || ((Number) id).longValue() <= 0) {
            throw new IllegalArgumentException("Invalid JWT identity");
        }
        return new OrderPrincipal(((Number) id).longValue(), claims.getSubject());
    }
}
