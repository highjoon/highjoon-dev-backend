package com.highjoondev.api.category.dto;

import com.highjoondev.api.category.entity.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, String title, UUID parentId, Instant createdAt) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(), category.getTitle(), category.getParentId(), category.getCreatedAt());
    }
}
