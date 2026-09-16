package com.ecommerce.productservice;

import com.ecommerce.productservice.controller.ProductController;
import com.ecommerce.productservice.exception.GlobalExceptionHandler;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.CategoryService;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-1 / BUG-2 regression: normal lookups by id and by SKU must only return
 * active products — a soft-deleted product behaves as if it does not exist.
 */
class ProductLookupContractTest {
    private ProductRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        ProductService service = new ProductService(repository, mock(CategoryService.class));
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Product activeProduct() {
        return Product.builder().id(1L).name("Laptop").sku("LAP-001")
                .price(new BigDecimal("10.00")).stockQuantity(5)
                .category(Category.builder().id(1L).name("Computers").build())
                .active(true).build();
    }

    @Test
    void activeProductIsRetrievableById() throws Exception {
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(activeProduct()));
        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void inactiveProductByIdReturns404() throws Exception {
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 1"));
    }

    @Test
    void activeProductIsRetrievableBySku() throws Exception {
        when(repository.findBySkuAndActiveTrue("LAP-001")).thenReturn(Optional.of(activeProduct()));
        mvc.perform(get("/api/v1/products/sku/LAP-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void inactiveProductBySkuReturns404() throws Exception {
        when(repository.findBySkuAndActiveTrue("LAP-001")).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/products/sku/LAP-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with SKU: LAP-001"));
    }
}
