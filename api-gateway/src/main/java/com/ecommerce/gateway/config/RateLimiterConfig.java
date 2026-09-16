package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Redis-backed rate limiter configuration for the API Gateway.
 *
 * Strategy: Token bucket algorithm (built into Spring Cloud Gateway via
 * the Lua script stored in Redis).
 *
 * Limits:
 *  - Per-IP for public product/category endpoints: 60 req/min
 *  - Per-User (JWT sub) for authenticated endpoints: 120 req/min
 *
 * Tokens:
 *  replenishRate  — tokens added per second  (sustained throughput)
 *  burstCapacity  — maximum tokens the bucket can hold (peak tolerance)
 *  requestedTokens — tokens consumed per request (default 1)
 *
 * Redis key pattern set by Spring internally:
 *   request_rate_limiter.{keyResolverValue}.tokens
 *   request_rate_limiter.{keyResolverValue}.timestamp
 */
@Configuration
public class RateLimiterConfig {

    /**
     * {@code publicApiKeyResolver} — extracts the client IP address.
     * Used for unauthenticated product/category browse routes.
     * Falls back to "anonymous" if the header is absent (e.g. direct calls).
     */
    @Bean
    @Primary
    public KeyResolver publicApiKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "anonymous";
            }
            return Mono.just(ip);
        };
    }

    /**
     * {@code userKeyResolver} — extracts the authenticated user identity
     * from the {@code X-User-Id} header set by the AuthenticationFilter.
     * Used for protected endpoints (orders, cart, payments).
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? "user:" + userId : "anonymous");
        };
    }

    /**
     * Rate limiter for public browse APIs (product catalog, categories).
     * 30 req/sec replenish, burst up to 60 — comfortable for normal browsing,
     * tight enough to block automated scrapers.
     */
    @Bean(name = "publicRateLimiter")
    public RedisRateLimiter publicRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    /**
     * Rate limiter for authenticated user APIs (cart, orders, payments).
     * 20 req/sec replenish, burst up to 40 — for legitimate interactive use.
     */
    @Bean(name = "userRateLimiter")
    @Primary
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
