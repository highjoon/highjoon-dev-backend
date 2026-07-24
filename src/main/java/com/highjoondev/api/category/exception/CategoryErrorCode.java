package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;

public enum CategoryErrorCode implements ErrorCode {
    NOT_FOUND("CATEGORY_NOT_FOUND", "Category not found: %s"),
    PARENT_NOT_FOUND("CATEGORY_PARENT_NOT_FOUND", "Category parent not found: %s"),
    INVALID_PARENT("CATEGORY_INVALID_PARENT", "Category cannot be its own parent or descendant: %s"),
    DUPLICATED_SLUG("DUPLICATED_SLUG", "Duplicated slug: %s");

    private final String code;
    private final String message;

    CategoryErrorCode(String code, String message) {
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
