package com.highjoondev.api.category.dto;

import java.util.UUID;

public interface CategoryMenuRow {
    UUID getId();

    String getTitle();

    String getSlug();

    UUID getParentId();
}
