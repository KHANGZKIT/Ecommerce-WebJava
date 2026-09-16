package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryDto;
import com.ecommerce.productservice.dto.CreateCategoryRequest;
import com.ecommerce.productservice.exception.CategoryAlreadyExistsException;
import com.ecommerce.productservice.exception.CategoryInUseException;
import com.ecommerce.productservice.exception.CategoryNotFoundException;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllActiveCategories() {
        return categoryRepository.findAllByActiveTrue()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new CategoryAlreadyExistsException("Category '" + request.getName() + "' already exists");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build());
        log.info("Created category: {}", saved.getName());
        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CreateCategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findOrThrow(id);
        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new CategoryInUseException("Cannot delete category '" + category.getName()
                    + "' (id: " + id + ") because active products reference it");
        }
        category.setActive(false);  // soft delete
        categoryRepository.save(category);
        log.info("Soft-deleted category id: {}", id);
    }

    public Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }

    private CategoryDto toDto(Category c) {
        return CategoryDto.builder()
                .id(c.getId()).name(c.getName())
                .description(c.getDescription()).imageUrl(c.getImageUrl())
                .active(c.isActive()).createdAt(c.getCreatedAt())
                .build();
    }
}
