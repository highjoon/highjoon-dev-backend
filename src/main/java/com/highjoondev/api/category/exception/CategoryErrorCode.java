package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;

public enum CategoryErrorCode implements ErrorCode {
    NOT_FOUND("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다: %s"),
    PARENT_NOT_FOUND("CATEGORY_PARENT_NOT_FOUND", "부모 카테고리를 찾을 수 없습니다: %s"),
    INVALID_PARENT("CATEGORY_INVALID_PARENT", "카테고리는 자기 자신이나 자손을 부모로 지정할 수 없습니다: %s"),
    DUPLICATED_SLUG("DUPLICATED_SLUG", "이미 사용 중인 slug입니다: %s");

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
