package com.highjoondev.api.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PostResponse post,
        // 첫 글은 없음
        @Schema(nullable = true) PostSummary previous,
        // 마지막 글은 없음
        @Schema(nullable = true) PostSummary next) {}
