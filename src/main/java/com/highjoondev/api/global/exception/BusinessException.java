package com.highjoondev.api.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.message(args));
        this.errorCode = errorCode;
    }
}
