package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for product-service.
 *
 * Key improvements:
 *  - {@link #getProductsByIds} replaces N individual calls with one batch request.
 *  - {@link #decrementStockAsync} triggers a non-blocking PATCH; failures are
 *    logged but do NOT block the checkout response (compensated by a scheduled job).
 *  - {@link #holdStock} atomically reserves inventory with durable operation marker.
 *  - {@link #releaseStock} releases held inventory on checkout failure.
 */
@Slf4j
@Component
public class ProductClient {

    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public ProductClient(WebClient.Builder builder,
                         @Value("${services.product-service.url}") String url) {
        this(builder, url, Duration.ofSeconds(3));
    }

    public ProductClient(WebClient.Builder builder, String url, Duration timeout) {
        this.webClient = builder.baseUrl(url).build();
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
    }

    /**
     * Batch fetch — single round-trip to product-service.
     * Returns Map<productId, ProductResponse> for O(1) lookup in the caller.
     */
    public Map<Long, ProductResponse> getProductsByIds(List<Long> ids) {
        try {
            return webClient.post()
                    .uri("/api/v1/products/batch")
                    .bodyValue(ids)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<Long, ProductResponse>>() {})
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("product-service batch fetch error: {}", ex.getMessage());
            throw new RuntimeException("Product service unavailable during batch fetch");
        }
    }

    /**
     * Single-product fetch — used by CartService to get live product details on item addition.
     */
    public ProductResponse getProductById(Long productId) {
        log.debug("Fetching product {} from product-service", productId);
        try {
            return webClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Product service error fetching product {}: status={}, body={}",
                    productId, ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = "Product service error: " + ex.getStatusCode();
            }
            throw new ApiException(message, status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching product {}: {}", productId, ex.getMessage());
            throw new ApiException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Atomically reserves stock in product-service using durable operation marker.
     */
    public HoldStockResponse holdStock(HoldStockRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("HoldStockRequest cannot be null");
        }
        log.info("Calling product-service /hold for operationKey={}", request.getCheckoutOperationKey());
        try {
            return webClient.post()
                    .uri("/api/v1/products/hold")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(HoldStockResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("product-service /hold error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = "Product service error: " + ex.getStatusCode();
            }
            throw new ApiException(message, status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("product-service communication error: {}", ex.getMessage());
            throw new ApiException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Releases held stock in product-service using the original operation key.
     */
    public ReleaseStockResponse releaseStock(ReleaseStockRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ReleaseStockRequest cannot be null");
        }
        log.info("Calling product-service /release for operationKey={}", request.getCheckoutOperationKey());
        try {
            return webClient.post()
                    .uri("/api/v1/products/release")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ReleaseStockResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("product-service /release error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String message = ex.getResponseBodyAsString();
            if (message == null || message.isBlank()) {
                message = "Product service error: " + ex.getStatusCode();
            }
            throw new ApiException(message, status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("product-service communication error: {}", ex.getMessage());
            throw new ApiException("Product service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Decrement stock — fire-and-forget (subscribe without blocking).
     * Failures are logged; a reconciliation job should handle stragglers.
     */
    public void decrementStockAsync(Long productId, int quantity) {
        webClient.patch()
                .uri("/api/v1/products/{id}/inventory", productId)
                .bodyValue(java.util.Map.of("quantity", -quantity, "mode", "DELTA"))
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    resp -> log.info("Decremented stock for product {} by {}", productId, quantity),
                    err  -> log.error("Failed to decrement stock for product {}: {}", productId, err.getMessage())
                );
    }
}
