package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.exception.ApiException;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductAlreadyExistsException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.productservice.model.ProductOperationMarker;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductOperationService productOperationService;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryService categoryService, ProductOperationService productOperationService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productOperationService = productOperationService;
    }

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this(productRepository, categoryService, null);
    }

    // -------------------------------------------------------------------------
    // Read operations (cacheable)
    // -------------------------------------------------------------------------

    /**
     * Paginated product list — cached per page/sort combination.
     * Key encodes page number + size + sort so different pages don't collide.
     */
    @Cacheable(value = "productsList", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findAllByActiveTrue(pageable).map(this::toDto);
    }

    @Cacheable(value = "productsList",
               key = "'cat-' + #categoryId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findAllByCategoryIdAndActiveTrue(categoryId, pageable).map(this::toDto);
    }

    /**
     * Full-text search — NOT cached because keyword space is too large.
     * If you need caching here, add a short TTL with a bounded key.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable).map(this::toDto);
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        return toDto(productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id)));
    }

    @Cacheable(value = "products", key = "'sku-' + #sku")
    @Transactional(readOnly = true)
    public ProductDto getProductBySku(String sku) {
        return toDto(productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku)));
    }

    /**
     * Batch fetch by IDs — used by order-service to replace N+1 remote calls.
     * Returns a Map<id, ProductDto> for O(1) lookup in the caller.
     * Not cached because this is an internal/system endpoint that changes on
     * every write; individual products are cached separately.
     */
    @Transactional(readOnly = true)
    public Map<Long, ProductDto> getProductsByIds(List<Long> ids) {
        return productRepository.findAllByIdInAndActiveTrue(ids)
                .stream()
                .collect(Collectors.toMap(p -> p.getId(), this::toDto));
    }

    // -------------------------------------------------------------------------
    // Write operations — invalidate instead of re-put: a write must not leave
    // either the id entry or the derived 'sku-…' entry stale, and a deactivating
    // write must not cache an inactive product as if it were retrievable.
    // -------------------------------------------------------------------------

    @CacheEvict(value = "productsList", allEntries = true)
    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ProductAlreadyExistsException("Product with SKU '" + request.getSku() + "' already exists");
        }
        Category category = categoryService.findOrThrow(request.getCategoryId());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(category)
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        log.info("Created product: {} (SKU: {})", saved.getName(), saved.getSku());
        return toDto(saved);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", key = "'sku-' + #result.sku"),
        @CacheEvict(value = "productsList", allEntries = true)
    })
    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getCategoryId() != null) {
            product.setCategory(categoryService.findOrThrow(request.getCategoryId()));
        }
        if (request.getActive() != null) product.setActive(request.getActive());
        return toDto(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", key = "'sku-' + #result.sku"),
        @CacheEvict(value = "productsList", allEntries = true)
    })
    @Transactional
    public ProductDto updateInventory(Long id, InventoryUpdateRequest request) {
        Product product = findOrThrow(id);
        long newQty = switch (request.getMode()) {
            case ABSOLUTE -> request.getQuantity();
            case DELTA    -> (long) product.getStockQuantity() + request.getQuantity();
        };
        if (newQty < 0) {
            throw new InsufficientStockException(
                "Insufficient stock for product id " + id + ". Current: " + product.getStockQuantity());
        }
        if (newQty > Integer.MAX_VALUE) {
            throw new InsufficientStockException("Stock quantity exceeds supported maximum for product id " + id);
        }
        product.setStockQuantity((int) newQty);
        log.info("Updated inventory for product {} to {}", id, newQty);
        return toDto(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", key = "'sku-' + #result.sku"),
        @CacheEvict(value = "productsList", allEntries = true)
    })
    @Transactional
    public ProductDto deleteProduct(Long id) {
        Product product = findOrThrow(id);
        product.setActive(false);   // soft delete
        productRepository.save(product);
        log.info("Soft-deleted product id: {}", id);
        return toDto(product);
    }

    /**
     * Coordinates atomic stock hold with durable operation marker and idempotency recovery.
     * 1. Validates checkoutOperationKey and non-empty items.
     * 2. Aggregates items and computes canonical request fingerprint (SHA-256 of sorted productId:quantity).
     * 3. Fast-path check for existing marker (read-only): if HELD and fingerprint matches -> replay; if conflict -> 409.
     * 4. Attempts atomic hold execution via ProductOperationService.executeHoldStock in its own transaction.
     * 5. If concurrent race triggers DataIntegrityViolationException, catches it outside the failed transaction,
     *    reads the committed marker in a fresh read transaction, and replays or rejects as conflict.
     */
    public HoldStockResponse holdStock(HoldStockRequest request) {
        if (request == null) {
            throw new ApiException("Request cannot be null", HttpStatus.BAD_REQUEST);
        }
        if (request.getCheckoutOperationKey() == null || request.getCheckoutOperationKey().trim().isEmpty()) {
            throw new ApiException("Checkout operation key is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCheckoutOperationKey().trim().length() > 128) {
            throw new ApiException("Checkout operation key must not exceed 128 characters", HttpStatus.BAD_REQUEST);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException("Items list cannot be empty", HttpStatus.BAD_REQUEST);
        }

        String key = request.getCheckoutOperationKey().trim();

        Map<Long, Integer> aggregated = new HashMap<>();
        for (StockItemRequest item : request.getItems()) {
            if (item.getProductId() == null) {
                throw new ApiException("Product ID is required", HttpStatus.BAD_REQUEST);
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ApiException("Quantity must be greater than zero for product id: " + item.getProductId(), HttpStatus.BAD_REQUEST);
            }
            try {
                aggregated.merge(item.getProductId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new ApiException("Quantity overflow for product id: " + item.getProductId(), HttpStatus.BAD_REQUEST);
            }
        }

        String fingerprint = computeFingerprint(aggregated);

        // Fast-path read check
        Optional<ProductOperationMarker> existing = productOperationService.findMarker(key);
        if (existing.isPresent()) {
            return productOperationService.replayOrConflict(existing.get(), fingerprint);
        }

        try {
            return productOperationService.executeHoldStock(request, aggregated, fingerprint);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent hold race detected for key {}: {}", key, ex.getMessage());
            return productOperationService.findMarker(key)
                    .map(marker -> productOperationService.replayOrConflict(marker, fingerprint))
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Coordinates atomic stock release using the durable operation marker.
     * Validates operation key and delegates to ProductOperationService to restore exact marker quantities.
     */
    public ReleaseStockResponse releaseStock(ReleaseStockRequest request) {
        if (request == null) {
            throw new ApiException("Request cannot be null", HttpStatus.BAD_REQUEST);
        }
        if (request.getCheckoutOperationKey() == null || request.getCheckoutOperationKey().trim().isEmpty()) {
            throw new ApiException("Checkout operation key is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCheckoutOperationKey().trim().length() > 128) {
            throw new ApiException("Checkout operation key must not exceed 128 characters", HttpStatus.BAD_REQUEST);
        }

        return productOperationService.executeReleaseStock(request);
    }

    public static String computeFingerprint(Map<Long, Integer> aggregated) {
        String canonical = aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    private ProductDto toDto(Product p) {
        CategoryDto categoryDto = CategoryDto.builder()
                .id(p.getCategory().getId())
                .name(p.getCategory().getName())
                .description(p.getCategory().getDescription())
                .imageUrl(p.getCategory().getImageUrl())
                .active(p.getCategory().isActive())
                .build();

        return ProductDto.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .sku(p.getSku()).price(p.getPrice()).stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl()).category(categoryDto).active(p.isActive())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
