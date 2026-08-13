package com.highjoondev.api.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();

    String message(Object... args);

    HttpStatus status();
}
