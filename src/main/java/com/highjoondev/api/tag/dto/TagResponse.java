package com.highjoondev.api.tag.dto;

import com.highjoondev.api.tag.entity.Tag;
import java.time.Instant;
import java.util.UUID;

public record TagResponse(UUID id, String name, long postCount, Instant createdAt) {
    public static TagResponse from(Tag tag, long postCount) {
        return new TagResponse(tag.getId(), tag.getName(), postCount, tag.getCreatedAt());
    }
}
