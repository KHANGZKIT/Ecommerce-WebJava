package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.config.WebClientConfig;
import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.exception.GlobalExceptionHandler;
import com.ecommerce.cartservice.exception.ProductServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ProductClientTest {

    private static HttpServer mockProductServer;
    private static String serverUrl;
    private static final WebClientConfig webClientConfig = new WebClientConfig();

    @BeforeAll
    static void startServer() throws IOException {
        mockProductServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // 1. Success 200
        mockProductServer.createContext("/api/v1/products/1", exchange -> {
            byte[] body = ("{\"id\":1,\"name\":\"Wireless Mouse\",\"sku\":\"MOUSE-01\","
                    + "\"price\":25.50,\"stockQuantity\":10,\"active\":true}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        // 2. 404 Not Found
        mockProductServer.createContext("/api/v1/products/404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        // 3. 500 Internal Server Error
        mockProductServer.createContext("/api/v1/products/500", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        // 4. Timeout (slow server response)
        mockProductServer.createContext("/api/v1/products/888", exchange -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ignored) {
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        // 5. Decode / malformed response failure
        mockProductServer.createContext("/api/v1/products/777", exchange -> {
            byte[] body = "not-json".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        mockProductServer.start();
        int port = mockProductServer.getAddress().getPort();
        serverUrl = "http://127.0.0.1:" + port;
    }

    @AfterAll
    static void stopServer() {
        if (mockProductServer != null) {
            mockProductServer.stop(0);
        }
    }

    @Test
    void getProductById_successReturnsProduct() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl);
        ProductResponse response = client.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Wireless Mouse", response.getName());
        assertEquals("MOUSE-01", response.getSku());
        assertEquals(new BigDecimal("25.50"), response.getPrice());
        assertTrue(response.isActive());
    }

    @Test
    void getProductById_notFoundReturns404() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl);
        ProductServiceException ex = assertThrows(ProductServiceException.class,
                () -> client.getProductById(404L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

        // Verify GlobalExceptionHandler maps to HTTP 404
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResp = handler.handleApiException(ex);
        assertEquals(404, errorResp.getStatusCode().value());
    }

    @Test
    void getProductById_serverErrorReturns503() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl);
        ProductServiceException ex = assertThrows(ProductServiceException.class,
                () -> client.getProductById(500L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());

        // Verify GlobalExceptionHandler maps to HTTP 503
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResp = handler.handleApiException(ex);
        assertEquals(503, errorResp.getStatusCode().value());
    }

    @Test
    void getProductById_connectionFailureReturns503() {
        // Closed port on localhost
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), "http://127.0.0.1:1");
        ProductServiceException ex = assertThrows(ProductServiceException.class,
                () -> client.getProductById(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResp = handler.handleApiException(ex);
        assertEquals(503, errorResp.getStatusCode().value());
    }

    @Test
    void getProductById_timeoutReturns503AndDoesNotHangIndefinitely() {
        // Fast timeout (1 second) to test against 4-second delay
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(1));

        long start = System.currentTimeMillis();
        ProductServiceException ex = assertThrows(ProductServiceException.class,
                () -> client.getProductById(888L));
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 3500, "Request should time out without blocking indefinitely. Elapsed: " + duration + "ms");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResp = handler.handleApiException(ex);
        assertEquals(503, errorResp.getStatusCode().value());
    }

    @Test
    void getProductById_decodeFailureReturns503() {
        ProductClient client = new ProductClient(webClientConfig.webClientBuilder(), serverUrl);
        ProductServiceException ex = assertThrows(ProductServiceException.class,
                () -> client.getProductById(777L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> errorResp = handler.handleApiException(ex);
        assertEquals(503, errorResp.getStatusCode().value());
    }

    @Test
    void getProductById_unexpectedProgrammingBugIsNotDisguisedAs503() {
        WebClient.Builder mockBuilder = org.mockito.Mockito.mock(WebClient.Builder.class);
        WebClient mockClient = org.mockito.Mockito.mock(WebClient.class);
        org.mockito.Mockito.when(mockBuilder.baseUrl(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockBuilder);
        org.mockito.Mockito.when(mockBuilder.build()).thenReturn(mockClient);
        org.mockito.Mockito.when(mockClient.get()).thenThrow(new NullPointerException("simulated programming bug"));

        ProductClient client = new ProductClient(mockBuilder, "http://localhost");
        assertThrows(NullPointerException.class, () -> client.getProductById(1L));
    }
}


