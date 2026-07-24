package com.highjoondev.api.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CategoryCreateRequest(
        @Schema(description = "카테고리 제목", example = "프론트엔드") @NotBlank(message = "제목은 필수입니다") String title,

        @Schema(description = "카테고리 Slug", example = "frontend") @NotBlank(message = "slug은 필수입니다") String slug,

        @Schema(description = "부모 카테고리 ID (없으면 최상위 카테고리)") UUID parentId) {}
