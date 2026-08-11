package com.highjoondev.api.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "공통 응답 양식")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(boolean success, T data, ApiError error, Instant timestamp) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null, Instant.now());
    }

    public static <T> ApiResult<T> error(String code, String message) {
        return new ApiResult<>(false, null, new ApiError(code, message, List.of()), Instant.now());
    }

    public static <T> ApiResult<T> error(String code, String message, List<String> details) {
        return new ApiResult<>(false, null, new ApiError(code, message, details), Instant.now());
    }
}
