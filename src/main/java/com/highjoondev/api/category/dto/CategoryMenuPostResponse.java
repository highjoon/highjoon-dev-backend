package com.highjoondev.api.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryMenuPostResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug) {}
