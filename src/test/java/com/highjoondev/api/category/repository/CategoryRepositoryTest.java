package com.highjoondev.api.category.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.category.entity.Category;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class CategoryRepositoryTest {
    @Autowired
    CategoryRepository categoryRepository;

    @Test
    @DisplayName("카테고리 저장 시 id, 생성일 자동 생성")
    void save_withValidCategory_shouldPersistAndBeRetrievable() {
        // Given
        Category category = Category.builder().title("title").slug("title").build();

        // When
        categoryRepository.saveAndFlush(category);

        // Then
        assertThat(category.getId()).isNotNull();
        assertThat(category.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("중복 slug 저장 시 예외")
    void save_withDuplicateSlug_shouldThrowException() {
        // Given
        categoryRepository.saveAndFlush(
                Category.builder().title("title").slug("slug").build());
        Category duplicate =
                Category.builder().title("other title").slug("slug").build();

        // When, Then
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("slug 존재 여부 확인")
    void existsBySlug_withExistingSlug_shouldReturnTrue() {
        // Given
        categoryRepository.saveAndFlush(
                Category.builder().title("title").slug("slug").build());

        // When, Then
        assertThat(categoryRepository.existsBySlug("slug")).isTrue();
        assertThat(categoryRepository.existsBySlug("other-slug")).isFalse();
    }

    @Test
    @DisplayName("자기 자신 제외 slug 중복 검사")
    void existsBySlugAndIdNot_withOwnSlug_shouldReturnFalse() {
        // Given
        Category category = Category.builder().title("title").slug("slug").build();
        categoryRepository.saveAndFlush(category);

        // When, Then
        assertThat(categoryRepository.existsBySlugAndIdNot("slug", category.getId()))
                .isFalse();
        assertThat(categoryRepository.existsBySlugAndIdNot("slug", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    @DisplayName("카테고리 삭제")
    void delete_withValidCategory_shouldBeDeleted() {
        // Given
        Category category = Category.builder().title("title").slug("title").build();
        categoryRepository.saveAndFlush(category);

        // When
        categoryRepository.delete(category);

        // Then
        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    @DisplayName("부모 삭제 시 하위 카테고리 함께 삭제")
    void delete_withChildren_shouldCascadeDeleteChildren() {
        // Given
        Category parent = Category.builder().title("parent").slug("parent").build();
        Category child =
                Category.builder().title("child").slug("child").parent(parent).build();
        categoryRepository.saveAndFlush(parent);
        categoryRepository.saveAndFlush(child);

        // When
        categoryRepository.delete(parent);
        categoryRepository.flush();

        // Then
        assertThat(categoryRepository.findById(parent.getId())).isEmpty();
        assertThat(categoryRepository.findById(child.getId())).isEmpty();
    }
}
