package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.WebClientConfig;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductClientHoldReleaseTest {

    private static HttpServer mockServer;
    private static String serverUrl;
    private static final WebClientConfig webClientConfig = new WebClientConfig();
    private static final AtomicReference<String> lastReceivedRequestBody = new AtomicReference<>();
    private static final AtomicReference<String> lastReceivedPath = new AtomicReference<>();

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        mockServer.createContext("/api/v1/products/hold", exchange -> {
            lastReceivedPath.set("/api/v1/products/hold");
            String bodyStr;
            try (InputStream is = exchange.getRequestBody()) {
                bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            lastReceivedRequestBody.set(bodyStr);

            if (bodyStr.contains("chk-conflict-key")) {
                byte[] body = "{\"error\":\"Operation key conflict: payload does not match existing hold for key chk-conflict-key\",\"status\":409}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(409, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            if (bodyStr.contains("chk-insufficient-stock")) {
                byte[] body = "{\"error\":\"Insufficient stock for product id: 1. Available: 0\",\"status\":400}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            byte[] body = ("{\"items\":[{\"productId\":1,\"name\":\"Wireless Mouse\",\"sku\":\"MOU-001\",\"price\":25.00,\"quantity\":2}],"
                    + "\"status\":\"HELD\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });

        mockServer.createContext("/api/v1/products/release", exchange -> {
            lastReceivedPath.set("/api/v1/products/release");
            String bodyStr;
            try (InputStream is = exchange.getRequestBody()) {
                bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            lastReceivedRequestBody.set(bodyStr);

            if (bodyStr.contains("chk-server-error")) {
                exchange.sendResponseHeaders(500, -1);
                return;
            }

            byte[] body = "{\"status\":\"RELEASED\",\"releasedItemCount\":2}".getBytes(StandardCharsets.UTF_8);
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
    void holdStock_success_returnsAuthoritativeItems() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("chk-op-12345")
                .items(List.of(StockItemRequest.builder().productId(1L).quantity(2).build()))
                .build();

        HoldStockResponse response = client.holdStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        HeldItemDto item = response.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(1L);
        assertThat(item.getName()).isEqualTo("Wireless Mouse");
        assertThat(item.getSku()).isEqualTo("MOU-001");
        assertThat(item.getPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(item.getQuantity()).isEqualTo(2);

        assertThat(lastReceivedPath.get()).isEqualTo("/api/v1/products/hold");
        assertThat(lastReceivedRequestBody.get()).contains("chk-op-12345");
        assertThat(lastReceivedRequestBody.get()).contains("\"productId\":1");
    }

    @Test
    void holdStock_whenConflict409_mapsToApiException409() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("chk-conflict-key")
                .items(List.of(StockItemRequest.builder().productId(1L).quantity(2).build()))
                .build();

        assertThatThrownBy(() -> client.holdStock(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getMessage()).contains("Operation key conflict");
                });
    }

    @Test
    void holdStock_whenInsufficientStock400_mapsToApiException400() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("chk-insufficient-stock")
                .items(List.of(StockItemRequest.builder().productId(1L).quantity(2).build()))
                .build();

        assertThatThrownBy(() -> client.holdStock(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).contains("Insufficient stock");
                });
    }

    @Test
    void holdStock_nullRequest_throwsIllegalArgumentException() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.holdStock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HoldStockRequest cannot be null");
    }

    @Test
    void releaseStock_success_returnsReleasedCount() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        ReleaseStockRequest request = new ReleaseStockRequest("chk-op-release-key");
        ReleaseStockResponse response = client.releaseStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(response.getReleasedItemCount()).isEqualTo(2);

        assertThat(lastReceivedPath.get()).isEqualTo("/api/v1/products/release");
        assertThat(lastReceivedRequestBody.get()).contains("chk-op-release-key");
    }

    @Test
    void releaseStock_whenServerError_mapsToApiException() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        ReleaseStockRequest request = new ReleaseStockRequest("chk-server-error");

        assertThatThrownBy(() -> client.releaseStock(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @Test
    void releaseStock_nullRequest_throwsIllegalArgumentException() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.releaseStock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReleaseStockRequest cannot be null");
    }
}
