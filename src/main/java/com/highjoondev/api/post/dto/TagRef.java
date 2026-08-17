package com.highjoondev.api.post.dto;

import com.highjoondev.api.tag.entity.Tag;
import java.util.UUID;

public record TagRef(UUID id, String name) {
    public static TagRef from(Tag tag) {
        return new TagRef(tag.getId(), tag.getName());
    }
}
