package com.highjoondev.api.category.repository;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.category.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class CategoryRepositoryTest {
    @Autowired
    CategoryRepository categoryRepository;

    @Test
    void save_withValidCategory_shouldPersistAndBeRetrievable() {
        // Given
        Category category = Category.create("title", null);

        // When
        categoryRepository.saveAndFlush(category);

        // Then
        assertThat(category.getId()).isNotNull();
        assertThat(category.getCreatedAt()).isNotNull();
    }
}
