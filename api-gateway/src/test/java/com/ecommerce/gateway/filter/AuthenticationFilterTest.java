package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private AuthenticationFilter filter;
    private AuthenticationFilter.Config config;

    @BeforeEach
    void setUp() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", Encoders.BASE64.encode(key.getEncoded()));
        filter = new AuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        config = new AuthenticationFilter.Config();
        config.setRequireUserId(true);
    }

    private String token(Object userId, long expiresIn) {
        var builder = Jwts.builder().setSubject("a@example.com")
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expiresIn));
        if (userId != null) builder.claim("userId", userId);
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    private ServerWebExchange forward(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.apply(config).filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block(Duration.ofSeconds(3));
        return forwarded.get();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET /api/v1/cart", "POST /api/v1/cart/items",
            "PATCH /api/v1/cart/items/22", "DELETE /api/v1/cart/items/22", "DELETE /api/v1/cart"})
    void everyCartOperationReplacesSpoofedIdentity(String operation) {
        String[] parts = operation.split(" ");
        String token = token(1L, 60000);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.valueOf(parts[0]), parts[1])
                .header("Authorization", "Bearer " + token)
                .header("x-user-id", "2", "3")
                .header("x-user-email", "b@example.com", "c@example.com")
                .header("x-auth-token", "forged", "another"));
        var headers = assertNotNullForwarded(forward(exchange)).getRequest().getHeaders();
        assertEquals(java.util.List.of("1"), headers.get("X-User-Id"));
        assertEquals(java.util.List.of("a@example.com"), headers.get("X-User-Email"));
        assertEquals(java.util.List.of(token), headers.get("X-Auth-Token"));
        assertEquals("Bearer " + token, headers.getFirst("Authorization"));
    }

    private ServerWebExchange assertNotNullForwarded(ServerWebExchange result) {
        assertNotNull(result);
        return result;
    }

    @Test
    void validTokenNeedsNoClientIdentityHeaders() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart")
                .header("Authorization", "Bearer " + token(1L, 60000)));
        assertEquals("1", assertNotNullForwarded(forward(exchange)).getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongSignatureTokens() {
        String wrongKey = Jwts.builder().setSubject("a@example.com").claim("userId", 1)
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)).compact();
        for (String authorization : new String[]{"", "Basic credentials", "Bearer garbage",
                "Bearer " + token(1, -60000), "Bearer " + wrongKey}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart")
                    .header("Authorization", authorization).header("X-User-Id", "2"));
            assertNull(forward(exchange));
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
        var missing = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart").header("X-User-Id", "2"));
        assertNull(forward(missing));
        assertEquals(HttpStatus.UNAUTHORIZED, missing.getResponse().getStatusCode());
    }

    @Test
    void rejectsMissingAndInvalidUserIdClaimsWithoutHeaderFallback() {
        for (Object id : new Object[]{null, 0, -1, "1", 1.5, true}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart")
                    .header("Authorization", "Bearer " + token(id, 60000)).header("X-User-Id", "2"));
            assertNull(forward(exchange));
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    @Test
    void rejectsAmbiguousAuthorizationHeaders() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/cart")
                .header("Authorization", "Bearer " + token(1, 60000), "Bearer " + token(2, 60000)));
        assertNull(forward(exchange));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void legacyTokenOnOtherRoutesCannotForwardSpoofedUserId() {
        config.setRequireUserId(false);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me")
                .header("Authorization", "Bearer " + token(null, 60000)).header("X-User-Id", "2"));
        var headers = assertNotNullForwarded(forward(exchange)).getRequest().getHeaders();
        assertNull(headers.getFirst("X-User-Id"));
        assertEquals("a@example.com", headers.getFirst("X-User-Email"));
    }

    @Test
    void deployedCartRouteRequiresUserId() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        var properties = yaml.getObject();
        String route = properties.stringPropertyNames().stream()
                .filter(name -> name.endsWith(".id") && "cart-service".equals(properties.getProperty(name)))
                .findFirst().orElseThrow();
        String prefix = route.substring(0, route.length() - 3);
        assertEquals("AuthenticationFilter", properties.getProperty(prefix + ".filters[1].name"));
        assertEquals("true", properties.getProperty(prefix + ".filters[1].args.requireUserId"));
    }
}