package com.highjoondev.api.post.dto;

import com.highjoondev.api.tag.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record TagRef(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
    public static TagRef from(Tag tag) {
        return new TagRef(tag.getId(), tag.getName());
    }
}
