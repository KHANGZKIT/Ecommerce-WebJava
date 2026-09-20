package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
public class PaymentClient {

    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public PaymentClient(WebClient.Builder builder,
                         @Value("${services.payment-service.url}") String url) {
        this(builder, url, Duration.ofSeconds(3));
    }

    public PaymentClient(WebClient.Builder builder, String url, Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
    }

    public PaymentResponse chargePayment(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("PaymentRequest cannot be null");
        }
        log.info("Calling payment-service for order {}", request.getOrderId());
        try {
            return webClient.post()
                    .uri("/api/v1/payments/charge")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                log.error("Payment service 5xx server error for order {}: status={}", request.getOrderId(), ex.getStatusCode());
                return PaymentResponse.builder()
                        .orderId(request.getOrderId())
                        .status(PaymentResponse.STATUS_UNKNOWN)
                        .message("Payment service 5xx server error: " + ex.getStatusCode())
                        .build();
            }
            log.error("Payment service client error for order {}: status={}", request.getOrderId(), ex.getStatusCode());
            throw ex;
        } catch (Exception ex) {
            log.error("Ambiguous transport failure calling payment-service for order {}: {}", request.getOrderId(), ex.getMessage());
            return PaymentResponse.builder()
                    .orderId(request.getOrderId())
                    .status(PaymentResponse.STATUS_UNKNOWN)
                    .message("Ambiguous transport failure: " + ex.getMessage())
                    .build();
        }
    }
}

