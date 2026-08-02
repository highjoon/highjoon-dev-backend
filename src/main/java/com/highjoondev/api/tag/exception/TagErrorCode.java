package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.ErrorCode;

public enum TagErrorCode implements ErrorCode {
    NOT_FOUND("TAG_NOT_FOUND", "태그를 찾을 수 없습니다: %s"),
    DUPLICATED_NAME("TAG_DUPLICATED_NAME", "이미 사용 중인 태그 이름입니다: %s");

    private final String code;
    private final String message;

    TagErrorCode(String code, String message) {
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
