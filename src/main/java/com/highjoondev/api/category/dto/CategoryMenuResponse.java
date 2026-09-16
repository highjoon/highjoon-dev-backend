package com.highjoondev.api.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record CategoryMenuResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        // 게시물이 없으면 빈 배열
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CategoryMenuPostResponse> posts,
        // 하위 카테고리가 없으면 빈 배열
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CategoryMenuResponse> children) {}
