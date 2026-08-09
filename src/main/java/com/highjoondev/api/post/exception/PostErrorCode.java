package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;

public enum PostErrorCode implements ErrorCode {
    NOT_FOUND("POST_NOT_FOUND", "게시물을 찾을 수 없습니다: %s"),
    DUPLICATED_SLUG("POST_DUPLICATED_SLUG", "이미 사용 중인 slug입니다: %s"),
    DUPLICATED_FEATURED_POST("POST_DUPLICATED_FEATURED", "이미 추천 게시물이 있습니다: %s"),
    FEATURED_NOT_FOUND("POST_FEATURED_NOT_FOUND", "추천 게시물이 존재하지 않습니다");

    private final String code;
    private final String message;

    PostErrorCode(String code, String message) {
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
