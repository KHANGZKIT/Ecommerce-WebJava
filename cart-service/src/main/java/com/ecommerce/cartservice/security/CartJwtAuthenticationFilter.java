package com.ecommerce.cartservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Installed only inside Spring Security, avoiding duplicate servlet-filter registration. */
public class CartJwtAuthenticationFilter extends OncePerRequestFilter {
    private final CartJwtVerifier verifier;
    private final AuthenticationEntryPoint entryPoint;

    public CartJwtAuthenticationFilter(CartJwtVerifier verifier, AuthenticationEntryPoint entryPoint) {
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
                CartPrincipal principal = verifier.verify(token);
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
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
