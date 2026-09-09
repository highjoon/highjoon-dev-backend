package com.highjoondev.api.tag.dto;

import com.highjoondev.api.tag.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record TagResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long postCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {
    public static TagResponse from(Tag tag, long postCount) {
        return new TagResponse(tag.getId(), tag.getName(), postCount, tag.getCreatedAt());
    }
}
