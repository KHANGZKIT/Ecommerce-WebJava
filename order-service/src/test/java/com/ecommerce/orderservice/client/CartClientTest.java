package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.WebClientConfig;
import com.ecommerce.orderservice.dto.CartItemResponse;
import com.ecommerce.orderservice.dto.CartResponse;
import com.ecommerce.orderservice.exception.ApiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartClientTest {

    private static HttpServer mockServer;
    private static String serverUrl;
    private static final WebClientConfig webClientConfig = new WebClientConfig();
    private static final AtomicReference<String> lastReceivedAuthHeader = new AtomicReference<>();
    private static final AtomicReference<String> lastReceivedMethod = new AtomicReference<>();

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        mockServer.createContext("/api/v1/cart", exchange -> {
            lastReceivedMethod.set(exchange.getRequestMethod());
            lastReceivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            // Mock 401 if bad token
            if (authHeader != null && authHeader.contains("invalid-token")) {
                byte[] body = "{\"error\":\"Invalid bearer token\",\"status\":401}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(401, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            // Mock 503 if unavailable token
            if (authHeader != null && authHeader.contains("503-token")) {
                byte[] body = "{\"error\":\"Cart service unavailable\",\"status\":503}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            // Handle DELETE (clearCart)
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (authHeader != null && authHeader.contains("error-token")) {
                    exchange.sendResponseHeaders(500, -1);
                    return;
                }
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Handle GET
            if (authHeader != null && authHeader.contains("empty-token")) {
                byte[] body = "{\"id\":10,\"userId\":42,\"items\":[],\"totalPrice\":0.00,\"itemCount\":0,\"updatedAt\":\"2026-09-17T12:00:00\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            // Standard success GET response
            byte[] body = ("{\"id\":10,\"userId\":42,\"items\":[{\"id\":101,\"productId\":1,"
                    + "\"productName\":\"Wireless Mouse\",\"productSku\":\"MOU-001\",\"unitPrice\":25.00,"
                    + "\"quantity\":2,\"subtotal\":50.00,\"imageUrl\":\"http://img.com/1.jpg\"}],"
                    + "\"totalPrice\":50.00,\"itemCount\":2,\"updatedAt\":\"2026-09-17T12:00:00\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        mockServer.start();
        int port = mockServer.getAddress().getPort();
        serverUrl = "http://127.0.0.1:" + port;
    }

    @AfterAll
    static void stopServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void getCart_success_returnsCartWithItemsAndForwardsBearerToken() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));
        CartResponse response = client.getCart("valid-jwt-token-xyz");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("50.00");
        assertThat(response.getItems()).hasSize(1);

        CartItemResponse item = response.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("Wireless Mouse");
        assertThat(item.getProductSku()).isEqualTo("MOU-001");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("25.00");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo("50.00");

        // Verify that the Authorization header was correctly forwarded
        assertThat(lastReceivedAuthHeader.get()).isEqualTo("Bearer valid-jwt-token-xyz");
        assertThat(lastReceivedMethod.get()).isEqualTo("GET");
    }

    @Test
    void getCart_bearerPrefixHandledGracefully() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));
        client.getCart("Bearer already-has-bearer-prefix");

        assertThat(lastReceivedAuthHeader.get()).isEqualTo("Bearer already-has-bearer-prefix");
    }

    @Test
    void getCart_emptyCart_returnsZeroItems() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));
        CartResponse response = client.getCart("empty-token");

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getItemCount()).isEqualTo(0);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void getCart_whenUnauthorized401_mapsToApiException401() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.getCart("invalid-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void getCart_whenServiceUnavailable503_mapsToApiException503() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.getCart("503-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void getCart_nullOrBlankToken_throwsIllegalArgumentException() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.getCart(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer token is required");

        assertThatThrownBy(() -> client.getCart("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer token is required");
    }

    @Test
    void clearCart_success_sendsDeleteWithBearerToken() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));
        client.clearCart("clear-token-123");

        assertThat(lastReceivedAuthHeader.get()).isEqualTo("Bearer clear-token-123");
        assertThat(lastReceivedMethod.get()).isEqualTo("DELETE");
    }

    @Test
    void clearCart_whenError_mapsToApiException() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.clearCart("error-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    void clearCart_nullOrBlankToken_throwsIllegalArgumentException() {
        CartClient client = new CartClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.clearCart(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer token is required");
    }
}
