package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public CategoryNotFoundException(UUID id) {
        super(ErrorCode.CATEGORY_NOT_FOUND.message(id));
        this.errorCode = ErrorCode.CATEGORY_NOT_FOUND;
    }
}
