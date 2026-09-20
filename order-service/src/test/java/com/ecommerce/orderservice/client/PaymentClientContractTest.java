package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.WebClientConfig;
import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.dto.PaymentResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentClientContractTest {

    private static HttpServer mockServer;
    private static String serverUrl;
    private static java.util.concurrent.ExecutorService serverExecutor;
    private static final WebClientConfig webClientConfig = new WebClientConfig();
    private static final AtomicInteger requestCounter = new AtomicInteger(0);

    @BeforeAll
    static void startServer() throws IOException {
        serverExecutor = java.util.concurrent.Executors.newCachedThreadPool();
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockServer.setExecutor(serverExecutor);

        mockServer.createContext("/api/v1/payments/charge", exchange -> {
            requestCounter.incrementAndGet();
            String bodyStr;
            try (InputStream is = exchange.getRequestBody()) {
                bodyStr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (bodyStr.contains("TIMEOUT")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            if (bodyStr.contains("SERVER_ERROR")) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }

            if (bodyStr.contains("DECLINE")) {
                byte[] body = ("{\"id\":102,\"transactionId\":\"TXN-FAIL\",\"orderId\":99,"
                        + "\"userId\":1,\"amount\":50.00,\"paymentMethod\":\"CREDIT_CARD\","
                        + "\"status\":\"FAILED\",\"message\":\"Payment declined by bank. Please try again.\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                exchange.close();
                return;
            }

            byte[] body = ("{\"id\":101,\"transactionId\":\"TXN-SUCCESS-123\",\"orderId\":50,"
                    + "\"userId\":1,\"amount\":50.00,\"paymentMethod\":\"CREDIT_CARD\","
                    + "\"status\":\"SUCCESS\",\"message\":\"Payment processed successfully\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
            exchange.close();
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
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void chargePayment_success_returnsSuccessResponse() {
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(50L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        PaymentResponse response = client.chargePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isFailed()).isFalse();
        assertThat(response.isUnknown()).isFalse();
        assertThat(response.getStatus()).isEqualTo(PaymentResponse.STATUS_SUCCESS);
        assertThat(response.getTransactionId()).isEqualTo("TXN-SUCCESS-123");
        assertThat(response.getOrderId()).isEqualTo(50L);
    }

    @Test
    void chargePayment_declined_returnsFailedResponse() {
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(99L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("DECLINE")
                .build();

        PaymentResponse response = client.chargePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isFailed()).isTrue();
        assertThat(response.isUnknown()).isFalse();
        assertThat(response.getStatus()).isEqualTo(PaymentResponse.STATUS_FAILED);
        assertThat(response.getMessage()).contains("Payment declined by bank");
    }

    @Test
    void chargePayment_serverError5xx_returnsUnknownResponse() {
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(2));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("SERVER_ERROR")
                .build();

        PaymentResponse response = client.chargePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isFailed()).isFalse();
        assertThat(response.isUnknown()).isTrue();
        assertThat(response.getStatus()).isEqualTo(PaymentResponse.STATUS_UNKNOWN);
        assertThat(response.getOrderId()).isEqualTo(100L);
    }

    @Test
    void chargePayment_transportTimeout_returnsUnknownResponse() {
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofMillis(500));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("TIMEOUT")
                .build();

        PaymentResponse response = client.chargePayment(request);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isFailed()).isFalse();
        assertThat(response.isUnknown()).isTrue();
        assertThat(response.getStatus()).isEqualTo(PaymentResponse.STATUS_UNKNOWN);
        assertThat(response.getOrderId()).isEqualTo(101L);
    }

    @Test
    void chargePayment_singleAttemptNoAutomaticRetry() {
        requestCounter.set(0);
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(1));

        PaymentRequest request = PaymentRequest.builder()
                .orderId(102L)
                .userId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod("SERVER_ERROR")
                .build();

        PaymentResponse response = client.chargePayment(request);

        assertThat(response.isUnknown()).isTrue();
        // Verifies exactly ONE network call was attempted (no retry loop)
        assertThat(requestCounter.get()).isEqualTo(1);
    }

    @Test
    void chargePayment_nullRequest_throwsIllegalArgumentException() {
        PaymentClient client = new PaymentClient(webClientConfig.webClientBuilder(), serverUrl, Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.chargePayment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PaymentRequest cannot be null");
    }
}
