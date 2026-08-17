package com.highjoondev.api.tag.service;

import com.highjoondev.api.tag.dto.TagCreateRequest;
import com.highjoondev.api.tag.dto.TagResponse;
import com.highjoondev.api.tag.dto.TagUpdateRequest;
import com.highjoondev.api.tag.entity.Tag;
import com.highjoondev.api.tag.exception.DuplicatedTagNameException;
import com.highjoondev.api.tag.exception.TagNotFoundException;
import com.highjoondev.api.tag.repository.TagRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {
    private final TagRepository tagRepository;

    @Transactional
    public TagResponse create(TagCreateRequest request) {
        String name = normalizeName(request.name());
        if (tagRepository.existsByName(name)) {
            throw new DuplicatedTagNameException(name);
        }

        Tag tag = Tag.builder().name(name).build();
        tagRepository.save(tag);

        return TagResponse.from(tag, 0);
    }

    public List<TagResponse> findAll() {
        return tagRepository.findAllWithPostCount();
    }

    public TagResponse findById(UUID id) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        return TagResponse.from(tag, tagRepository.countPostsByTagId(id));
    }

    public TagResponse findByName(String name) {
        String normalized = normalizeName(name);
        Tag tag = tagRepository.findByName(normalized).orElseThrow(() -> new TagNotFoundException(normalized));
        return TagResponse.from(tag, tagRepository.countPostsByTagId(tag.getId()));
    }

    @Transactional
    public TagResponse updateById(UUID id, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));

        String name = normalizeName(request.name());
        if (tagRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicatedTagNameException(name);
        }

        tag.update(name);

        return TagResponse.from(tag, tagRepository.countPostsByTagId(id));
    }

    @Transactional
    public void deleteById(UUID id) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
        tagRepository.delete(tag);
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
