package com.highjoondev.api.global.exception;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", "Invalid input"),
    INVALID_PARAMETER("INVALID_PARAMETER", "Invalid parameter: %s");

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
