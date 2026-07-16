package com.highjoondev.api.category.service;

import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.dto.CategoryUpdateRequest;
import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_withValidRequest_shouldReturnCategoryResponse() {
        // Given
        var request = new CategoryCreateRequest("title", null);

        // When
        CategoryResponse response = categoryService.create(request);

        // Then
        assertThat(response.title()).isEqualTo("title");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_withNonExistentParentId_shouldThrowException() {
        // Given
        var request = new CategoryCreateRequest("title", UUID.randomUUID());
        when(categoryRepository.findById(request.parentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request)).isInstanceOf(CategoryParentNotFoundException.class);
    }

    @Test
    void findById_whenCategoryNotFound_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        // 스터빙(stubbing): findById(id)가 호출되면 Optional.empty()를 반환하도록 미리 정의.
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> categoryService.findById(id)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void findById_whenCategoryFound_shouldReturnCategoryResponse() {
        // Given
        UUID id = UUID.randomUUID();
        Category category = Category.create("title", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        // When
        CategoryResponse response = categoryService.findById(id);

        // Then
        assertThat(response.title()).isEqualTo("title");
        assertThat(response.parentId()).isNull();
    }

    @Test
    void findAll_shouldReturnAllCategories() {
        // Given
        Category category = Category.create("title", null);
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        // When
        List<CategoryResponse> responses = categoryService.findAll();

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("title");
    }

    @Test
    void findAll_whenNoCategories_shouldReturnEmptyList() {
        // Given
        when(categoryRepository.findAll()).thenReturn(List.of());

        // When
        List<CategoryResponse> responses = categoryService.findAll();

        // Then
        assertThat(responses).isEmpty();
    }

    @Test
    void create_withParentId_shouldSaveWithParentId() {
        // Given
        UUID parentId = UUID.randomUUID();
        var request = new CategoryCreateRequest("title", parentId);
        Category parentCategory = Category.create("parent", null);
        ReflectionTestUtils.setField(parentCategory, "id", parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parentCategory));

        // When
        CategoryResponse response = categoryService.create(request);

        // Then
        assertThat(response.parentId()).isEqualTo(parentId);
    }

    @Test
    void update_withValidRequest_shouldSaveWithParentId() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", parentId);

        Category targetCategory = Category.create("old title", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));

        Category parentCategory = Category.create("parent", null);
        ReflectionTestUtils.setField(parentCategory, "id", parentId);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parentCategory));

        // When
        CategoryResponse response = categoryService.updateById(id, request);

        // Then
        assertThat(response.parentId()).isEqualTo(parentId);
    }

    @Test
    void update_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", parentId);

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void update_withNonExistentParentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", parentId);

        Category targetCategory = Category.create("old title", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));
        when(categoryRepository.findById(parentId)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request))
                .isInstanceOf(CategoryParentNotFoundException.class);
    }

    @Test
    void update_withSelfAsParentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", id);

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request))
                .isInstanceOf(CategoryInvalidParentException.class);
    }

    @Test
    void update_withNullParentId_shouldRemoveParent() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", null);

        Category targetCategory = Category.create("old title", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));

        // When
        CategoryResponse response = categoryService.updateById(id, request);

        // Then
        assertThat(response.parentId()).isNull();
        assertThat(response.title()).isEqualTo("title");
    }

    @Test
    void delete_withValidRequest_shouldRemoveCategory() {
        // Given
        UUID id = UUID.randomUUID();
        Category category = Category.create("title", null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        // When
        categoryService.deleteById(id);

        // Then
        verify(categoryRepository).delete(category);

    }

    @Test
    void delete_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        when( categoryRepository.findById(id)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> categoryService.deleteById(id)).isInstanceOf(CategoryNotFoundException.class);
    }
}
