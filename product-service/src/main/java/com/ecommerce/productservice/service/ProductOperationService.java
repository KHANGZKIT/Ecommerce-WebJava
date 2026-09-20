package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.HeldItemDto;
import com.ecommerce.productservice.dto.HoldStockRequest;
import com.ecommerce.productservice.dto.HoldStockResponse;
import com.ecommerce.productservice.dto.ReleaseStockRequest;
import com.ecommerce.productservice.dto.ReleaseStockResponse;
import com.ecommerce.productservice.exception.ApiException;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.model.ProductOperationItem;
import com.ecommerce.productservice.model.ProductOperationMarker;
import com.ecommerce.productservice.model.ProductOperationStatus;
import com.ecommerce.productservice.repository.ProductOperationMarkerRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductOperationService {

    private final ProductRepository productRepository;
    private final ProductOperationMarkerRepository markerRepository;

    @Transactional(readOnly = true)
    public Optional<ProductOperationMarker> findMarker(String checkoutOperationKey) {
        return markerRepository.findByCheckoutOperationKey(checkoutOperationKey);
    }

    /**
     * Executes atomic stock hold and persists the durable HELD operation marker in ONE local database transaction.
     * 1. Verifies marker absence within the transaction.
     * 2. Decrements stock conditionally in ascending productId order.
     * 3. Fetches authoritative DB product state.
     * 4. Persists the HELD operation marker with exact item snapshots and request fingerprint.
     * If any stock update or marker insertion fails, the entire transaction rolls back.
     */
    @Transactional(rollbackFor = Exception.class)
    public HoldStockResponse executeHoldStock(HoldStockRequest request, Map<Long, Integer> aggregated, String fingerprint) {
        String key = request.getCheckoutOperationKey().trim();

        // Re-check within transaction in case another transaction committed just before this one started
        Optional<ProductOperationMarker> existing = markerRepository.findByCheckoutOperationKey(key);
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), fingerprint);
        }

        List<Long> sortedProductIds = aggregated.keySet().stream()
                .sorted()
                .toList();

        // 1. Conditional stock updates in deterministic ascending order
        for (Long productId : sortedProductIds) {
            int quantity = aggregated.get(productId);
            int updated = productRepository.holdStock(productId, quantity);
            if (updated == 0) {
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    throw new ProductNotFoundException("Product not found with id: " + productId);
                }
                if (!product.isActive()) {
                    throw new ApiException("Product is not active with id: " + productId, HttpStatus.BAD_REQUEST);
                }
                throw new InsufficientStockException("Insufficient stock for product id " + productId +
                        ". Available: " + product.getStockQuantity() + ", requested: " + quantity);
            }
        }

        // 2. Fetch authoritative product snapshots
        Map<Long, Product> productMap = productRepository.findAllById(sortedProductIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<ProductOperationItem> operationItems = new ArrayList<>(sortedProductIds.size());
        List<HeldItemDto> heldItems = new ArrayList<>(sortedProductIds.size());

        for (Long productId : sortedProductIds) {
            Product product = productMap.get(productId);
            int qty = aggregated.get(productId);

            operationItems.add(ProductOperationItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .price(product.getPrice())
                    .quantity(qty)
                    .build());

            heldItems.add(HeldItemDto.builder()
                    .productId(productId)
                    .name(product.getName())
                    .sku(product.getSku())
                    .price(product.getPrice())
                    .quantity(qty)
                    .build());
        }

        // 3. Create and persist durable HELD marker
        ProductOperationMarker marker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .requestFingerprint(fingerprint)
                .items(operationItems)
                .build();

        markerRepository.saveAndFlush(marker);
        log.info("Held stock and committed HELD marker for key: {} ({} products)", key, sortedProductIds.size());

        return new HoldStockResponse(heldItems);
    }

    /**
     * Executes atomic stock release using the durable marker in ONE local database transaction.
     * 1. Acquires row write lock (SELECT FOR UPDATE) on the marker.
     * 2. If marker missing, rejects with 404 Not Found.
     * 3. If marker is RELEASED, performs idempotent no-op replay.
     * 4. If marker is HELD, restores exact persisted quantities from marker, sets status to RELEASED.
     */
    @Transactional(rollbackFor = Exception.class)
    public ReleaseStockResponse executeReleaseStock(ReleaseStockRequest request) {
        String key = request.getCheckoutOperationKey().trim();

        ProductOperationMarker marker = markerRepository.findByCheckoutOperationKeyForUpdate(key)
                .orElseThrow(() -> new ApiException("No hold operation found for key: " + key, HttpStatus.NOT_FOUND));

        if (marker.getStatus() == ProductOperationStatus.RELEASED) {
            log.info("Operation key {} is already RELEASED; replaying release result without stock mutation", key);
            return new ReleaseStockResponse("RELEASED", marker.getItems().size());
        }

        // Marker is HELD. Restore exact held quantities stored in the durable marker.
        List<ProductOperationItem> itemsToRelease = marker.getItems().stream()
                .sorted(Comparator.comparing(ProductOperationItem::getProductId))
                .toList();

        for (ProductOperationItem item : itemsToRelease) {
            int updated = productRepository.releaseStock(item.getProductId(), item.getQuantity(), Integer.MAX_VALUE);
            if (updated == 0) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product == null) {
                    throw new ProductNotFoundException("Product not found with id: " + item.getProductId());
                }
                throw new ApiException("Stock quantity exceeds supported maximum for product id " + item.getProductId(), HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        marker.setStatus(ProductOperationStatus.RELEASED);
        marker.setUpdatedAt(LocalDateTime.now());
        markerRepository.saveAndFlush(marker);

        log.info("Released stock for {} items and updated marker to RELEASED for key: {}", itemsToRelease.size(), key);
        return new ReleaseStockResponse("RELEASED", itemsToRelease.size());
    }

    public HoldStockResponse replayOrConflict(ProductOperationMarker marker, String requestFingerprint) {
        if (marker.getStatus() == ProductOperationStatus.RELEASED) {
            throw new ApiException("Operation key has already been released: " + marker.getCheckoutOperationKey(), HttpStatus.CONFLICT);
        }
        if (!marker.getRequestFingerprint().equals(requestFingerprint)) {
            throw new ApiException("Operation key conflict: payload does not match existing hold for key " + marker.getCheckoutOperationKey(), HttpStatus.CONFLICT);
        }

        List<HeldItemDto> items = marker.getItems().stream()
                .map(item -> HeldItemDto.builder()
                        .productId(item.getProductId())
                        .name(item.getProductName())
                        .sku(item.getProductSku())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        log.info("Replaying existing hold result for key: {}", marker.getCheckoutOperationKey());
        return new HoldStockResponse(items);
    }
}
