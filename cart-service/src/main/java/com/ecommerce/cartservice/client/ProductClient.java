package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.exception.ProductServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * WebClient-based REST client for product-service.
 * Handles 404 / service-down / timeouts gracefully without crashing the cart flow.
 */
@Slf4j
@Component
public class ProductClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final Duration timeout;

    public ProductClient(WebClient.Builder builder,
                         @Value("${services.product-service.url}") String productServiceUrl,
                         @Value("${services.product-service.timeout:3s}") Duration timeout) {
        this.webClient = builder.baseUrl(productServiceUrl).build();
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    public ProductClient(WebClient.Builder builder, String productServiceUrl) {
        this(builder, productServiceUrl, DEFAULT_TIMEOUT);
    }

    public ProductResponse getProductById(Long productId) {
        log.debug("Fetching product {} from product-service", productId);
        try {
            ProductResponse response = webClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .timeout(timeout)
                    .onErrorMap(TimeoutException.class,
                            ex -> new ProductServiceException("Product service timed out", HttpStatus.SERVICE_UNAVAILABLE))
                    .block();

            if (response == null) {
                log.error("Product service returned empty response for product {}", productId);
                throw new ProductServiceException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return response;
        } catch (ProductServiceException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductServiceException("Product not found with id: " + productId, HttpStatus.NOT_FOUND);
            }
            log.error("Product service error for product {}: status={}", productId, ex.getStatusCode(), ex);
            throw new ProductServiceException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (WebClientException | CodecException ex) {
            log.error("Communication error with product-service for product {}: {}", productId, ex.getMessage(), ex);
            throw new ProductServiceException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
