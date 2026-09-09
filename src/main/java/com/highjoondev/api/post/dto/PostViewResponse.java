package com.highjoondev.api.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostViewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int viewCount) {
    public static PostViewResponse from(int viewCount) {
        return new PostViewResponse(viewCount);
    }
}
