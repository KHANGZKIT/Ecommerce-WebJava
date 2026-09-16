package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global JWT authentication filter applied to all protected routes.
 * Validates Bearer tokens and forwards X-User-* headers to downstream services.
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for: {}", request.getURI());
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            if (request.getHeaders().get(HttpHeaders.AUTHORIZATION).size() != 1) {
                return onError(exchange, "Multiple Authorization headers are not allowed", HttpStatus.UNAUTHORIZED);
            }
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            final String username;
            final Long userId;
            try {
                // Parse once: all propagated identity must come from the same verified claims.
                Claims claims = jwtUtil.extractAllClaims(token);
                username = claims.getSubject();
                if (username == null || username.isBlank() || claims.getExpiration() == null
                        || !claims.getExpiration().after(new Date())) {
                    throw new IllegalArgumentException("Missing or expired identity");
                }
                Object id = claims.get("userId");
                if (id == null) {
                    userId = null;
                } else if ((id instanceof Integer || id instanceof Long) && ((Number) id).longValue() > 0) {
                    userId = ((Number) id).longValue();
                } else {
                    throw new IllegalArgumentException("Invalid userId claim");
                }
                if (config.isRequireUserId() && userId == null) {
                    return onError(exchange, "Token lacks userId; please log in again", HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception ex) {
                return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
            }

            // These headers are trusted only on requests arriving through this filter.
            // Remove every client value (including duplicates) before setting verified values.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Email");
                        headers.remove("X-Auth-Token");
                        headers.set("X-User-Email", username);
                        headers.set("X-Auth-Token", token);
                        if (userId != null) {
                            headers.set("X-User-Id", userId.toString());
                        }
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\",\"status\":" + status.value() + "}").getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    public static class Config {
        private boolean requireUserId;

        public boolean isRequireUserId() { return requireUserId; }
        public void setRequireUserId(boolean requireUserId) { this.requireUserId = requireUserId; }
    }
}
