package com.highjoondev.api.post.dto;

import com.highjoondev.api.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record CategoryRef(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        // 최상위 카테고리는 없음
        @Schema(nullable = true) ParentRef parent) {

    public static CategoryRef from(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryRef(
                category.getId(), category.getSlug(), category.getTitle(), ParentRef.from(category.getParent()));
    }
}
