package com.ecommerce.paymentservice.client;

import com.ecommerce.paymentservice.dto.PaymentCallbackNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Value("${services.order-service.url:http://order-service:8084}") String orderServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean notifyPaymentResult(Long orderId, PaymentCallbackNotification notification) {
        log.info("Notifying order-service for order {} with payment status: {}", orderId, notification.getStatus());
        try {
            restClient.post()
                    .uri("/api/v1/orders/{id}/payment-callback", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notification)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully notified order-service for order {}", orderId);
            return true;
        } catch (Exception ex) {
            log.error("Failed to notify order-service for order {}: {}", orderId, ex.getMessage(), ex);
            return false;
        }
    }
}
