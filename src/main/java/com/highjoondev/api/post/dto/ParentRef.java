package com.highjoondev.api.post.dto;

import com.highjoondev.api.category.entity.Category;
import java.util.UUID;

public record ParentRef(UUID id, String slug, String title) {
    public static ParentRef from(Category parent) {
        return parent == null ? null : new ParentRef(parent.getId(), parent.getSlug(), parent.getTitle());
    }
}
