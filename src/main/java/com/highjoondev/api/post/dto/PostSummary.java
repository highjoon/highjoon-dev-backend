package com.highjoondev.api.post.dto;

import com.highjoondev.api.post.entity.Post;

public record PostSummary(String slug, String title) {
    public static PostSummary from(Post post) {
        return new PostSummary(post.getSlug(), post.getTitle());
    }
}
