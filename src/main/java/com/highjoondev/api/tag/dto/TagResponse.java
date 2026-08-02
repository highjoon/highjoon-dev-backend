package com.highjoondev.api.tag.dto;

import com.highjoondev.api.tag.entity.Tag;
import java.time.Instant;
import java.util.UUID;

public record TagResponse(UUID id, String name, Instant createdAt) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
