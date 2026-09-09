package com.highjoondev.api.post.dto;

import com.highjoondev.api.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ParentRef(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title) {
    public static ParentRef from(Category parent) {
        return parent == null ? null : new ParentRef(parent.getId(), parent.getSlug(), parent.getTitle());
    }
}
