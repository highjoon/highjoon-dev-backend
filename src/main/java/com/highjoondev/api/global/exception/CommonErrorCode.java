package com.highjoondev.api.global.exception;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "잘못된 입력입니다"),
    INVALID_PARAMETER("INVALID_PARAMETER", "잘못된 파라미터입니다: %s"),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다");

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
