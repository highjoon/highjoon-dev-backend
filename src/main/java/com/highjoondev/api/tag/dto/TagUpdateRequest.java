package com.highjoondev.api.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TagUpdateRequest(
        @Schema(description = "태그 이름", example = "react") @NotBlank(message = "이름은 필수입니다") String name) {}
