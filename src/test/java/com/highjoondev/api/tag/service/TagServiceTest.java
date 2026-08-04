package com.highjoondev.api.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.highjoondev.api.tag.dto.TagCreateRequest;
import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.dto.TagUpdateRequest;
import com.highjoondev.api.tag.entity.Tag;
import com.highjoondev.api.tag.exception.DuplicatedTagNameException;
import com.highjoondev.api.tag.exception.TagNotFoundException;
import com.highjoondev.api.tag.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    void create_withValidRequest_shouldReturnTagResponse() {
        // Given
        var request = new TagCreateRequest("react");

        // When
        TagResponse response = tagService.create(request);

        // Then
        assertThat(response.name()).isEqualTo("react");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void create_withDuplicateName_shouldThrowException() {
        // Given
        var request = new TagCreateRequest("react");
        when(tagRepository.existsByName("react")).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> tagService.create(request)).isInstanceOf(DuplicatedTagNameException.class);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void findById_whenTagFound_shouldReturnTagResponse() {
        // Given
        UUID id = UUID.randomUUID();
        Tag tag = Tag.builder().name("react").build();
        when(tagRepository.findById(id)).thenReturn(Optional.of(tag));

        // When
        TagResponse response = tagService.findById(id);

        // Then
        assertThat(response.name()).isEqualTo("react");
    }

    @Test
    void findById_whenTagNotFound_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        when(tagRepository.findById(id)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> tagService.findById(id)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void findAll_shouldReturnAllTags() {
        // Given
        when(tagRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(
                        Tag.builder().name("react").build(),
                        Tag.builder().name("nextjs").build()));

        // When
        List<TagResponse> responses = tagService.findAll();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("react");
    }

    @Test
    void findAll_whenNoTags_shouldReturnEmptyList() {
        // Given
        when(tagRepository.findAll(Sort.by("name"))).thenReturn(List.of());

        // When
        List<TagResponse> responses = tagService.findAll();

        // Then
        assertThat(responses).isEmpty();
    }

    @Test
    void update_withValidRequest_shouldReturnUpdatedTagResponse() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagRepository.findById(id))
                .thenReturn(Optional.of(Tag.builder().name("old name").build()));

        // When
        TagResponse response = tagService.updateById(id, request);

        // Then
        assertThat(response.name()).isEqualTo("react");
    }

    @Test
    void update_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagRepository.findById(id)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> tagService.updateById(id, request)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void update_withNonExistentIdAndDuplicateName_shouldThrowNotFound() {
        // Given: 존재하지 않는 id인데 이름은 중복 → 존재확인이 먼저이므로 404가 나와야 함
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagRepository.findById(id)).thenReturn(Optional.empty());
        lenient().when(tagRepository.existsByNameAndIdNot("react", id)).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> tagService.updateById(id, request)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void update_withDuplicateName_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagRepository.findById(id))
                .thenReturn(Optional.of(Tag.builder().name("old name").build()));
        when(tagRepository.existsByNameAndIdNot("react", id)).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> tagService.updateById(id, request)).isInstanceOf(DuplicatedTagNameException.class);
    }

    @Test
    void update_withOwnName_shouldSucceed() {
        // Given: 이름을 그대로 두고 저장해도 자기 자신은 중복이 아님
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("react");
        when(tagRepository.findById(id))
                .thenReturn(Optional.of(Tag.builder().name("react").build()));
        when(tagRepository.existsByNameAndIdNot("react", id)).thenReturn(false);

        // When
        TagResponse response = tagService.updateById(id, request);

        // Then
        assertThat(response.name()).isEqualTo("react");
    }

    @Test
    void delete_withValidId_shouldRemoveTag() {
        // Given
        UUID id = UUID.randomUUID();
        Tag tag = Tag.builder().name("react").build();
        when(tagRepository.findById(id)).thenReturn(Optional.of(tag));

        // When
        tagService.deleteById(id);

        // Then
        verify(tagRepository).delete(tag);
    }

    @Test
    void delete_withNonExistentId_shouldThrowException() {
        // Given
        UUID id = UUID.randomUUID();
        when(tagRepository.findById(id)).thenReturn(Optional.empty());

        // When, Then
        assertThatThrownBy(() -> tagService.deleteById(id)).isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void create_withMixedCaseName_shouldNormalize() {
        // Given
        var request = new TagCreateRequest("  React  ");

        // When
        TagResponse response = tagService.create(request);

        // Then
        assertThat(response.name()).isEqualTo("react");
        verify(tagRepository).existsByName("react");
    }

    @Test
    void create_withNameDifferingOnlyInCase_shouldThrowException() {
        // Given
        var request = new TagCreateRequest("REACT");
        when(tagRepository.existsByName("react")).thenReturn(true);

        // When, Then
        assertThatThrownBy(() -> tagService.create(request)).isInstanceOf(DuplicatedTagNameException.class);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void update_withMixedCaseName_shouldNormalize() {
        // Given
        UUID id = UUID.randomUUID();
        var request = new TagUpdateRequest("  React  ");
        when(tagRepository.findById(id))
                .thenReturn(Optional.of(Tag.builder().name("old-tag").build()));

        // When
        TagResponse response = tagService.updateById(id, request);

        // Then
        assertThat(response.name()).isEqualTo("react");
        verify(tagRepository).existsByNameAndIdNot("react", id);
    }

    @Test
    void findByName_withMixedCaseName_shouldNormalize() {
        // Given
        when(tagRepository.findByName("react"))
                .thenReturn(Optional.of(Tag.builder().name("react").build()));

        // When
        TagResponse response = tagService.findByName("  React  ");

        // Then
        assertThat(response.name()).isEqualTo("react");
    }
}
