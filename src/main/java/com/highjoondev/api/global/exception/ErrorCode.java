package com.highjoondev.api.global.exception;

public enum ErrorCode {
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "Category not found: %s"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Invalid input"),
    INVALID_PARAMETER("INVALID_PARAMETER", "Invalid parameter: %s");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message(Object... args) {
        return String.format(message, args);
    }
}
