package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PostErrorCode implements ErrorCode {
    NOT_FOUND("POST_NOT_FOUND", "게시물을 찾을 수 없습니다: %s", HttpStatus.NOT_FOUND),
    DUPLICATED_SLUG("POST_DUPLICATED_SLUG", "이미 사용 중인 slug입니다: %s", HttpStatus.CONFLICT),
    DUPLICATED_FEATURED_POST("POST_DUPLICATED_FEATURED", "이미 추천 게시물이 있습니다: %s", HttpStatus.CONFLICT),
    FEATURED_NOT_FOUND("POST_FEATURED_NOT_FOUND", "추천 게시물이 존재하지 않습니다", HttpStatus.NOT_FOUND),
    FEATURED_CANNOT_BE_HIDDEN("POST_FEATURED_CANNOT_BE_HIDDEN", "추천 게시물은 숨길 수 없습니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    PostErrorCode(String code, String message, HttpStatus status) {
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
