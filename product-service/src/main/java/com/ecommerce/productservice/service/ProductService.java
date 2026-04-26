package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.*;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductAlreadyExistsException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

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
        return toDto(findOrThrow(id));
    }

    @Cacheable(value = "products", key = "'sku-' + #sku")
    @Transactional(readOnly = true)
    public ProductDto getProductBySku(String sku) {
        return toDto(productRepository.findBySku(sku)
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
    // Write operations (cache maintaining)
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

    @Caching(
        put  = @CachePut(value = "products", key = "#id"),
        evict = @CacheEvict(value = "productsList", allEntries = true)
    )
    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request) {
        Product product = findOrThrow(id);
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getCategoryId() != null) {
            product.setCategory(categoryService.findOrThrow(request.getCategoryId()));
        }
        if (request.getActive() != null) product.setActive(request.getActive());
        return toDto(productRepository.save(product));
    }

    @Caching(
        put  = @CachePut(value = "products", key = "#id"),
        evict = @CacheEvict(value = "productsList", allEntries = true)
    )
    @Transactional
    public ProductDto updateInventory(Long id, InventoryUpdateRequest request) {
        Product product = findOrThrow(id);
        int newQty = switch (request.getMode()) {
            case ABSOLUTE -> request.getQuantity();
            case DELTA    -> product.getStockQuantity() + request.getQuantity();
        };
        if (newQty < 0) {
            throw new InsufficientStockException(
                "Insufficient stock for product id " + id + ". Current: " + product.getStockQuantity());
        }
        product.setStockQuantity(newQty);
        log.info("Updated inventory for product {} to {}", id, newQty);
        return toDto(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "productsList", allEntries = true)
    })
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findOrThrow(id);
        product.setActive(false);   // soft delete
        productRepository.save(product);
        log.info("Soft-deleted product id: {}", id);
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
