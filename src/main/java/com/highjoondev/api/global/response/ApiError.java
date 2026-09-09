package com.highjoondev.api.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ApiError(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> details) {}
