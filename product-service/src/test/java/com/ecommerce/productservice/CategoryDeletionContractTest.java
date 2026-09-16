package com.ecommerce.productservice;

import com.ecommerce.productservice.controller.CategoryController;
import com.ecommerce.productservice.exception.GlobalExceptionHandler;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-4 regression: a category still referenced by active products cannot be
 * soft-deleted (409 Conflict); a category free of active products can be.
 */
class CategoryDeletionContractTest {
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        productRepository = mock(ProductRepository.class);
        CategoryService service = new CategoryService(categoryRepository, productRepository);
        mvc = MockMvcBuilders.standaloneSetup(new CategoryController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(
                Category.builder().id(1L).name("Computers").active(true).build()));
    }

    @Test
    void categoryWithActiveProductsCannotBeDeleted() throws Exception {
        when(productRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(true);
        mvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete category 'Computers' (id: 1) because active products reference it"));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void categoryWithoutActiveProductsCanBeSoftDeleted() throws Exception {
        when(productRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(false);
        mvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }
}
