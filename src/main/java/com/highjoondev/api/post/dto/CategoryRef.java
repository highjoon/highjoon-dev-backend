package com.highjoondev.api.post.dto;

import com.highjoondev.api.category.entity.Category;
import java.util.UUID;

public record CategoryRef(UUID id, String slug, String title, ParentRef parent) {

    public static CategoryRef from(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryRef(
                category.getId(), category.getSlug(), category.getTitle(), ParentRef.from(category.getParent()));
    }
}
