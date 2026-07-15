package com.highjoondev.api.category.service;

import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.dto.CategoryUpdateRequest;
import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        Category parent = resolveParent(request.parentId());
        Category category = Category.create(request.title(), parent);
        categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse findById(UUID id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateById(UUID id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        Category parent = resolveParent(request.parentId());
        category.update(request.title(), parent);

        return CategoryResponse.from(category);
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new CategoryParentNotFoundException(parentId));
    }
}
