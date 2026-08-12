package com.highjoondev.api.post.dto;

import com.highjoondev.api.post.entity.Post;
import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String slug,
        String title,
        String description,
        String contentUrl,
        String bannerImageUrl,
        Instant publishedAt,
        int viewCount,
        CategoryRef category,
        Instant createdAt,
        Instant updatedAt) {
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
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
