package com.highjoondev.api.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ErrorResponse", description = "에러 응답")
public record ErrorResponse(@Schema(example = "false") boolean success, ApiError error, Instant timestamp) {}
