package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.CartResponse;
import com.ecommerce.orderservice.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
public class CartClient {

    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public CartClient(WebClient.Builder builder,
                      @Value("${services.cart-service.url:http://cart-service:8083}") String url) {
        this(builder, url, Duration.ofSeconds(3));
    }

    public CartClient(WebClient.Builder builder, String url, Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
    }

    public CartResponse getCart(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("Bearer token is required to access cart");
        }
        String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;
        try {
            return webClient.get()
                    .uri("/api/v1/cart")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .bodyToMono(CartResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("cart-service error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = "Cart service error: " + ex.getStatusCode();
            }
            throw new ApiException(message, status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("cart-service communication error: {}", ex.getMessage());
            throw new ApiException("Cart service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void clearCart(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("Bearer token is required to clear cart");
        }
        String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;
        try {
            webClient.delete()
                    .uri("/api/v1/cart")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("cart-service clearCart error: status={}", ex.getStatusCode());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            throw new ApiException("Failed to clear cart", status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("cart-service clearCart communication error: {}", ex.getMessage());
            throw new ApiException("Cart service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
