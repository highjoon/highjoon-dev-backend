package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TagErrorCode implements ErrorCode {
    NOT_FOUND("TAG_NOT_FOUND", "태그를 찾을 수 없습니다: %s", HttpStatus.NOT_FOUND),
    REFERENCE_NOT_FOUND("TAG_REFERENCE_NOT_FOUND", "지정한 태그를 찾을 수 없습니다: %s", HttpStatus.BAD_REQUEST),
    DUPLICATED_NAME("TAG_DUPLICATED_NAME", "이미 사용 중인 태그 이름입니다: %s", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    TagErrorCode(String code, String message, HttpStatus status) {
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
