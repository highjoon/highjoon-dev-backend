package com.highjoondev.api.category.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.category.entity.Category;
import java.util.UUID;
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
    void save_withValidCategory_shouldPersistAndBeRetrievable() {
        // Given
        Category category = Category.create("title", "title", null);

        // When
        categoryRepository.saveAndFlush(category);

        // Then
        assertThat(category.getId()).isNotNull();
        assertThat(category.getCreatedAt()).isNotNull();
    }

    @Test
    void save_withDuplicateSlug_shouldThrowException() {
        // Given
        categoryRepository.saveAndFlush(Category.create("title", "slug", null));
        Category duplicate = Category.create("other title", "slug", null);

        // When, Then
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySlug_withExistingSlug_shouldReturnTrue() {
        // Given
        categoryRepository.saveAndFlush(Category.create("title", "slug", null));

        // When, Then
        assertThat(categoryRepository.existsBySlug("slug")).isTrue();
        assertThat(categoryRepository.existsBySlug("other-slug")).isFalse();
    }

    @Test
    void existsBySlugAndIdNot_withOwnSlug_shouldReturnFalse() {
        // Given
        Category category = Category.create("title", "slug", null);
        categoryRepository.saveAndFlush(category);

        // When, Then
        assertThat(categoryRepository.existsBySlugAndIdNot("slug", category.getId()))
                .isFalse();
        assertThat(categoryRepository.existsBySlugAndIdNot("slug", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    void delete_withValidCategory_shouldBeDeleted() {
        // Given
        Category category = Category.create("title", "title", null);
        categoryRepository.saveAndFlush(category);

        // When
        categoryRepository.delete(category);

        // Then
        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    void delete_withChildren_shouldCascadeDeleteChildren() {
        // Given
        Category parent = Category.create("parent", "parent", null);
        Category child = Category.create("child", "child", parent);
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
