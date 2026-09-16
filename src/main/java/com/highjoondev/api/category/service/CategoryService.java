package com.highjoondev.api.category.service;

import com.highjoondev.api.category.dto.*;
import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.category.repository.CategoryRepository;
import com.highjoondev.api.post.dto.PostMenuRow;
import com.highjoondev.api.post.repository.PostRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicatedCategorySlugException(request.slug());
        }

        Category parent = resolveParent(request.parentId());
        Category category = Category.builder()
                .title(request.title())
                .slug(request.slug())
                .parent(parent)
                .build();
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

    public List<CategoryMenuResponse> findMenu() {
        List<PostMenuRow> postMenuRows = postRepository.findMenuRows();
        List<CategoryMenuRow> categoryMenuRows = categoryRepository.findMenuRows();

        // 카테고리 id 별 게시물. 쿼리의 발행일 내림차순을 그대로 유지
        Map<UUID, List<CategoryMenuPostResponse>> postsByCategoryId = new HashMap<>();
        postMenuRows.forEach(row -> postsByCategoryId
                .computeIfAbsent(row.getCategoryId(), key -> new ArrayList<>())
                .add(new CategoryMenuPostResponse(row.getTitle(), row.getSlug())));

        // TODO: categoryMenuRows 를 parentId 로 묶고, 루트부터 재귀로 트리 조립
        return List.of();
    }

    @Transactional
    public CategoryResponse updateById(UUID id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new DuplicatedCategorySlugException(request.slug());
        }

        Category parent = resolveParent(request.parentId());
        category.update(request.title(), request.slug(), parent);

        return CategoryResponse.from(category);
    }

    @Transactional
    public void deleteById(UUID id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        categoryRepository.delete(category);
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId).orElseThrow(() -> new CategoryParentNotFoundException(parentId));
    }
}
