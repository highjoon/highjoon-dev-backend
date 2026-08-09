package com.highjoondev.api.global.exception;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "잘못된 입력입니다"),
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 파라미터입니다: %s"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 경로를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다"),
    MALFORMED_REQUEST("MALFORMED_REQUEST", "요청 본문을 읽을 수 없습니다"),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다"),
    WRITE_NOT_ALLOWED("WRITE_NOT_ALLOWED", "쓰기 요청은 허용되지 않습니다");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message(Object... args) {
        return String.format(message, args);
    }
}
