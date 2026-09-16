package com.ecommerce.productservice;

import com.ecommerce.productservice.controller.ProductController;
import com.ecommerce.productservice.dto.InventoryUpdateRequest;
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
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryContractTest {
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
    void acceptsOrderNegativeDeltaAndSubtractsStock() throws Exception {
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-2,\"mode\":\"DELTA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(3));
        verify(repository).save(product);
    }

    @Test
    void positiveDeltaAddsAndAbsoluteReplacesStock() throws Exception {
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2,\"mode\":\"DELTA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(7));
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0,\"mode\":\"ABSOLUTE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void rejectsNegativeAbsoluteBeforePersistence() throws Exception {
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-2,\"mode\":\"ABSOLUTE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMissingFields() throws Exception {
        for (String body : new String[]{"{}", "{\"mode\":\"DELTA\"}", "{\"quantity\":1}"}) {
            mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInsufficientStockWithoutSaving() throws Exception {
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-6,\"mode\":\"DELTA\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(5, product.getStockQuantity());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsOverflowWithoutWrappingOrSaving() throws Exception {
        product.setStockQuantity(Integer.MAX_VALUE);
        mvc.perform(patch("/api/v1/products/1/inventory").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1,\"mode\":\"DELTA\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(Integer.MAX_VALUE, product.getStockQuantity());
        verify(repository, never()).save(any());
    }

    @Test
    void validationHelperDoesNotAddJsonField() throws Exception {
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setMode(InventoryUpdateRequest.UpdateMode.DELTA);
        request.setQuantity(-2);
        var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(request);
        assertEquals(2, json.size());
        assertFalse(json.has("quantityValidForMode"));
    }
}