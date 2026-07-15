package com.highjoondev.api.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryUpdateRequest(
        @Schema(description = "카테고리 제목", example = "프론트엔드") @NotBlank
        String title,

        @Schema(description = "부모 카테고리 ID (없으면 최상위 카테고리)") UUID parentId) {}
