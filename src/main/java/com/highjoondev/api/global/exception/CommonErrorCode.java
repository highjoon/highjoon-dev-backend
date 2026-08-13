package com.highjoondev.api.global.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "잘못된 입력입니다", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 파라미터입니다: %s", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 경로를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다", HttpStatus.METHOD_NOT_ALLOWED),
    MALFORMED_REQUEST("MALFORMED_REQUEST", "요청 본문을 읽을 수 없습니다", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    WRITE_NOT_ALLOWED("WRITE_NOT_ALLOWED", "쓰기 요청은 허용되지 않습니다", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    CommonErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message(Object... args) {
        return String.format(message, args);
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
