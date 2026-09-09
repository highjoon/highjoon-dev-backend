package com.highjoondev.api.category.dto;

import com.highjoondev.api.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        // 최상위 카테고리는 없음
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        UUID parentId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getTitle(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getCreatedAt());
    }
}
