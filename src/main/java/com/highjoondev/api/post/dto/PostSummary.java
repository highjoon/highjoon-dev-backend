package com.highjoondev.api.post.dto;

import com.highjoondev.api.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

public record PostSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title) {
    public static PostSummary from(Post post) {
        return new PostSummary(post.getSlug(), post.getTitle());
    }
}
