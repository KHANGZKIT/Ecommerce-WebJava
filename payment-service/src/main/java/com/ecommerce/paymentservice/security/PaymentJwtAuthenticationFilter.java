package com.ecommerce.paymentservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PaymentJwtAuthenticationFilter extends OncePerRequestFilter {
    private final PaymentJwtVerifier verifier;
    private final AuthenticationEntryPoint entryPoint;

    public PaymentJwtAuthenticationFilter(PaymentJwtVerifier verifier, AuthenticationEntryPoint entryPoint) {
        this.verifier = verifier;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (!headers.isEmpty()) {
            try {
                if (headers.size() != 1 || !headers.get(0).startsWith("Bearer ")) {
                    throw new IllegalArgumentException("Invalid Authorization header");
                }
                String token = headers.get(0).substring(7);
                if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
                    throw new IllegalArgumentException("Invalid bearer token format");
                }
                PaymentPrincipal principal = verifier.verify(token);
                var context = SecurityContextHolder.createEmptyContext();
                var authorities = principal.isAdmin()
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                                  new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        : List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, new BadCredentialsException("Invalid bearer token"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
