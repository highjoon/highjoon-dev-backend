package com.highjoondev.api.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.highjoondev.api.category.dto.CategoryCreateRequest;
import com.highjoondev.api.category.dto.CategoryResponse;
import com.highjoondev.api.category.dto.CategoryUpdateRequest;
import com.highjoondev.api.category.entity.Category;
import com.highjoondev.api.category.exception.CategoryInvalidParentException;
import com.highjoondev.api.category.exception.CategoryNotFoundException;
import com.highjoondev.api.category.exception.CategoryParentNotFoundException;
import com.highjoondev.api.category.exception.DuplicatedCategorySlugException;
import com.highjoondev.api.category.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_withValidRequest_shouldReturnCategoryResponse() {
        // Given
        var request = new CategoryCreateRequest("title", "slug", null);

        // When
        CategoryResponse response = categoryService.create(request);

        // Then
        assertThat(response.title()).isEqualTo("title");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_withNonExistentParentId_shouldThrowException() {
        // Given
        var request = new CategoryCreateRequest("title", "slug", UUID.randomUUID());
        when(categoryRepository.findById(request.parentId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request)).isInstanceOf(CategoryParentNotFoundException.class);
    }

    @Test
    void create_withDuplicateSlug_shouldThrowException() {
        // Given
        var request = new CategoryCreateRequest("title", "slug", null);
        when(categoryRepository.existsBySlug("slug")).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> categoryService.create(request)).isInstanceOf(DuplicatedCategorySlugException.class);
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
        Category category = Category.builder().title("title").slug("slug").build();
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
        Category category = Category.builder().title("title").slug("slug").build();
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
        var request = new CategoryCreateRequest("title", "slug", parentId);
        Category parentCategory =
                Category.builder().title("parent").slug("parent-slug").build();
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
        var request = new CategoryUpdateRequest("title", "slug", parentId);

        Category targetCategory =
                Category.builder().title("old title").slug("old-slug").build();
        ReflectionTestUtils.setField(targetCategory, "id", id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));

        Category parentCategory =
                Category.builder().title("parent").slug("parent-slug").build();
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
        var request = new CategoryUpdateRequest("title", "slug", parentId);

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void update_withNonExistentIdAndDuplicateSlug_shouldThrowNotFound() {
        // Given: 존재하지 않는 id인데 slug는 중복 → 존재확인이 먼저이므로 404가 나와야 함
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", null);
        lenient().when(categoryRepository.existsBySlugAndIdNot("slug", id)).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void update_withNonExistentParentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", parentId);

        Category targetCategory =
                Category.builder().title("old title").slug("old-slug").build();
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
        var request = new CategoryUpdateRequest("title", "slug", id);

        Category targetCategory =
                Category.builder().title("old title").slug("old-slug").build();
        ReflectionTestUtils.setField(targetCategory, "id", id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request))
                .isInstanceOf(CategoryInvalidParentException.class);
    }

    @Test
    void update_withDescendantAsParentId_shouldThrowException() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", childId);

        Category root = Category.builder().title("root").slug("root-slug").build();
        ReflectionTestUtils.setField(root, "id", rootId);
        Category child = Category.builder()
                .title("child")
                .slug("child-slug")
                .parent(root)
                .build();
        ReflectionTestUtils.setField(child, "id", childId);

        when(categoryRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(rootId, request))
                .isInstanceOf(CategoryInvalidParentException.class);
    }

    @Test
    void update_withDuplicateSlug_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", null);

        Category targetCategory =
                Category.builder().title("old title").slug("old-slug").build();
        ReflectionTestUtils.setField(targetCategory, "id", id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(targetCategory));
        when(categoryRepository.existsBySlugAndIdNot("slug", id)).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> categoryService.updateById(id, request))
                .isInstanceOf(DuplicatedCategorySlugException.class);
    }

    @Test
    void update_withNullParentId_shouldRemoveParent() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new CategoryUpdateRequest("title", "slug", null);

        Category targetCategory =
                Category.builder().title("old title").slug("old-slug").build();
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
        Category category = Category.builder().title("title").slug("slug").build();
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
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> categoryService.deleteById(id)).isInstanceOf(CategoryNotFoundException.class);
    }
}
