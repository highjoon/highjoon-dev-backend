package com.highjoondev.api.global.exception;

public interface ErrorCode {
    String code();

    String message(Object... args);
}
