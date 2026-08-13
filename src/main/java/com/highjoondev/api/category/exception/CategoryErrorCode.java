package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CategoryErrorCode implements ErrorCode {
    NOT_FOUND("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다: %s", HttpStatus.NOT_FOUND),
    PARENT_NOT_FOUND("CATEGORY_PARENT_NOT_FOUND", "부모 카테고리를 찾을 수 없습니다: %s", HttpStatus.BAD_REQUEST),
    INVALID_PARENT("CATEGORY_INVALID_PARENT", "카테고리는 자기 자신이나 자손을 부모로 지정할 수 없습니다: %s", HttpStatus.BAD_REQUEST),
    DUPLICATED_SLUG("CATEGORY_DUPLICATED_SLUG", "이미 사용 중인 slug입니다: %s", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    CategoryErrorCode(String code, String message, HttpStatus status) {
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
