package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for product-service.
 *
 * Key improvements:
 *  - {@link #getProductsByIds} replaces N individual calls with one batch request.
 *  - {@link #decrementStockAsync} triggers a non-blocking PATCH; failures are
 *    logged but do NOT block the checkout response (compensated by a scheduled job).
 */
@Slf4j
@Component
public class ProductClient {

    private final WebClient webClient;

    public ProductClient(WebClient.Builder builder,
                         @Value("${services.product-service.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
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
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("product-service batch fetch error: {}", ex.getMessage());
            throw new RuntimeException("Product service unavailable during batch fetch");
        }
    }

    /**
     * Legacy single-product fetch — kept for backwards compatibility;
     * prefer {@link #getProductsByIds} for order creation.
     */
    public ProductResponse getProductById(Long productId) {
        try {
            return webClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Product not found: " + productId);
            }
            log.error("product-service error: {}", ex.getMessage());
            throw new RuntimeException("Product service unavailable");
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

    /** Blocking variant kept for compatibility; prefer async above. */
    public void decrementStock(Long productId, int quantity) {
        try {
            webClient.patch()
                    .uri("/api/v1/products/{id}/inventory", productId)
                    .bodyValue(java.util.Map.of("quantity", -quantity, "mode", "DELTA"))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Decremented stock for product {} by {}", productId, quantity);
        } catch (Exception ex) {
            log.error("Failed to decrement stock for product {}: {}", productId, ex.getMessage());
        }
    }
}
