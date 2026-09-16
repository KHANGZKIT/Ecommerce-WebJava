package com.ecommerce.productservice;

import com.ecommerce.productservice.controller.ProductController;
import com.ecommerce.productservice.dto.UpdateProductRequest;
import com.ecommerce.productservice.exception.GlobalExceptionHandler;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.CategoryService;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-3 regression: general product update can no longer touch stockQuantity —
 * inventory changes are only possible via PATCH /api/v1/products/{id}/inventory,
 * whose ABSOLUTE/DELTA contract is preserved.
 */
class ProductUpdateContractTest {
    private ProductRepository repository;
    private Product product;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        product = Product.builder().id(1L).name("Laptop").sku("LAP-001")
                .price(new BigDecimal("10.00")).stockQuantity(5)
                .category(Category.builder().id(1L).name("Computers").build()).build();
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        ProductService service = new ProductService(repository, mock(CategoryService.class));
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void generalUpdateCannotModifyStock() throws Exception {
        mvc.perform(patch("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Laptop Pro\",\"price\":12.50,\"stockQuantity\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.stockQuantity").value(5));
        assertEquals(5, product.getStockQuantity());
        verify(repository).save(product);
    }

    @Test
    void updateRequestDtoHasNoStockField() {
        assertFalse(Arrays.stream(UpdateProductRequest.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equalsIgnoreCase("stockQuantity")),
                "UpdateProductRequest must not carry a stockQuantity field");
    }

    @Test
    void inventoryEndpointStillSupportsAbsoluteAndDelta() throws Exception {
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":42,\"mode\":\"ABSOLUTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(42));
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":8,\"mode\":\"DELTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(50));
    }
}
