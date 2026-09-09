package com.highjoondev.api.post.dto;

import com.highjoondev.api.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String contentUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bannerImageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant publishedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int viewCount,
        // 미분류 게시물은 없음
        @Schema(nullable = true) CategoryRef category,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TagRef> tags,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getDescription(),
                post.getContentUrl(),
                post.getBannerImageUrl(),
                post.getPublishedAt(),
                post.getViewCount(),
                CategoryRef.from(post.getCategory()),
                post.getPostTags().stream()
                        .map(postTag -> TagRef.from(postTag.getTag()))
                        .sorted(Comparator.comparing(TagRef::name))
                        .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
