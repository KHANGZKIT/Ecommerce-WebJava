package com.ecommerce.productservice;

import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.CategoryService;
import com.ecommerce.productservice.service.ProductService;
import com.ecommerce.productservice.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RISK-1 regression: the SKU-keyed cache entry ("sku-…") must be evicted by
 * every product write so SKU lookups never serve stale or inactive products.
 *
 * Runs ProductService through a real caching proxy (ConcurrentMapCacheManager)
 * with a mocked repository — no Redis required; repository hit counts prove
 * whether the cache was actually invalidated.
 */
class ProductCacheContractTest {

    @Configuration
    @EnableCaching
    static class CacheConfig {
        @Bean
        public ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        public CategoryService categoryService() {
            return mock(CategoryService.class);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        public ProductService productService(ProductRepository repository, CategoryService categoryService) {
            return new ProductService(repository, categoryService);
        }
    }

    private Product product() {
        return Product.builder().id(1L).name("Laptop").sku("LAP-001")
                .price(new BigDecimal("10.00")).stockQuantity(5)
                .category(Category.builder().id(1L).name("Computers").build())
                .active(true).build();
    }

    @Test
    void skuLookupDoesNotReturnStaleDataAfterUpdate() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(CacheConfig.class)) {
            ProductService service = ctx.getBean(ProductService.class);
            ProductRepository repository = ctx.getBean(ProductRepository.class);
            Product product = product();
            when(repository.findBySkuAndActiveTrue("LAP-001")).thenReturn(Optional.of(product));
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

            assertEquals(new BigDecimal("10.00"), service.getProductBySku("LAP-001").getPrice());

            UpdateProductRequest request = new UpdateProductRequest();
            request.setPrice(new BigDecimal("99.00"));
            ProductDto updated = service.updateProduct(1L, request);
            assertEquals(new BigDecimal("99.00"), updated.getPrice());

            ProductDto afterUpdate = service.getProductBySku("LAP-001");
            assertEquals(new BigDecimal("99.00"), afterUpdate.getPrice(),
                    "SKU lookup must not serve the stale cached price after an update");
            verify(repository, times(2)).findBySkuAndActiveTrue("LAP-001");
        }
    }

    @Test
    void skuLookupDoesNotReturnInactiveProductAfterSoftDelete() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(CacheConfig.class)) {
            ProductService service = ctx.getBean(ProductService.class);
            ProductRepository repository = ctx.getBean(ProductRepository.class);
            Product product = product();
            AtomicBoolean deleted = new AtomicBoolean(false);
            when(repository.findBySkuAndActiveTrue("LAP-001"))
                    .thenAnswer(inv -> deleted.get() ? Optional.empty() : Optional.of(product));
            when(repository.findByIdAndActiveTrue(1L))
                    .thenAnswer(inv -> deleted.get() ? Optional.empty() : Optional.of(product));
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenAnswer(call -> {
                deleted.set(true);   // mirrors the post-commit database state
                return call.getArgument(0);
            });

            assertNotNull(service.getProductBySku("LAP-001"));
            assertNotNull(service.getProductById(1L));

            service.deleteProduct(1L);

            assertThrows(ProductNotFoundException.class, () -> service.getProductBySku("LAP-001"),
                    "SKU lookup must not return the soft-deleted product, cached or not");
            assertThrows(ProductNotFoundException.class, () -> service.getProductById(1L),
                    "ID lookup must not return the soft-deleted product, cached or not");
            verify(repository, times(2)).findBySkuAndActiveTrue("LAP-001");
            verify(repository, times(2)).findByIdAndActiveTrue(1L);
        }
    }

    @Test
    void skuLookupIsRefreshedAfterInventoryChange() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(CacheConfig.class)) {
            ProductService service = ctx.getBean(ProductService.class);
            ProductRepository repository = ctx.getBean(ProductRepository.class);
            Product product = product();
            when(repository.findBySkuAndActiveTrue("LAP-001")).thenReturn(Optional.of(product));
            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

            assertEquals(5, service.getProductBySku("LAP-001").getStockQuantity());

            var request = new com.ecommerce.productservice.dto.InventoryUpdateRequest();
            request.setMode(com.ecommerce.productservice.dto.InventoryUpdateRequest.UpdateMode.DELTA);
            request.setQuantity(7);
            assertEquals(12, service.updateInventory(1L, request).getStockQuantity());

            assertEquals(12, service.getProductBySku("LAP-001").getStockQuantity(),
                    "SKU lookup must not serve stale stock after an inventory update");
            verify(repository, Mockito.times(2)).findBySkuAndActiveTrue("LAP-001");
        }
    }
}
