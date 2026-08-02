package com.highjoondev.api.tag.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.highjoondev.api.TestcontainersConfig;
import com.highjoondev.api.tag.entity.Tag;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public class TagRepositoryTest {
    @Autowired
    TagRepository tagRepository;

    @Test
    void save_withValidTag_shouldPersistAndBeRetrievable() {
        // Given
        Tag tag = Tag.create("test-tag");

        // When
        tagRepository.saveAndFlush(tag);

        // Then
        assertThat(tag.getId()).isNotNull();
        assertThat(tag.getCreatedAt()).isNotNull();
        assertThat(tag.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_withDuplicateName_shouldThrowException() {
        // Given
        tagRepository.saveAndFlush(Tag.create("test-tag"));
        Tag duplicate = Tag.create("test-tag");

        // When, Then
        assertThatThrownBy(() -> tagRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByName_withExistingName_shouldReturnTrue() {
        // Given
        tagRepository.saveAndFlush(Tag.create("test-tag"));

        // When, Then
        assertThat(tagRepository.existsByName("test-tag")).isTrue();
        assertThat(tagRepository.existsByName("absent-tag")).isFalse();
    }

    @Test
    void existsByNameAndIdNot_withOwnName_shouldReturnFalse() {
        // Given
        Tag tag = Tag.create("test-tag");
        tagRepository.saveAndFlush(tag);

        // When, Then
        assertThat(tagRepository.existsByNameAndIdNot("test-tag", tag.getId())).isFalse();
        assertThat(tagRepository.existsByNameAndIdNot("test-tag", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    void delete_withValidTag_shouldBeDeleted() {
        // Given
        Tag tag = Tag.create("test-tag");
        tagRepository.saveAndFlush(tag);

        // When
        tagRepository.delete(tag);

        // Then
        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    void findAll_withNameSort_shouldReturnSortedByName() {
        // Given
        tagRepository.saveAndFlush(Tag.create("zzz-tag"));
        tagRepository.saveAndFlush(Tag.create("aaa-tag"));

        // When
        List<String> names = tagRepository.findAll(Sort.by("name")).stream()
                .map(Tag::getName)
                .toList();

        // Then
        assertThat(names.indexOf("aaa-tag")).isLessThan(names.indexOf("zzz-tag"));
    }

    @Test
    void update_shouldChangeName() {
        // Given
        Tag tag = Tag.create("old-tag");
        tagRepository.saveAndFlush(tag);

        // When
        tag.update("test-tag");
        tagRepository.flush();

        // Then
        assertThat(tagRepository.findById(tag.getId()).orElseThrow().getName()).isEqualTo("test-tag");
    }
}
