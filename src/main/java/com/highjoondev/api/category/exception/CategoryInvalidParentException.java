package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryInvalidParentException extends RuntimeException {
    private final ErrorCode errorCode;

    public CategoryInvalidParentException(UUID id) {
        super(ErrorCode.CATEGORY_INVALID_PARENT.message(id));
        this.errorCode = ErrorCode.CATEGORY_INVALID_PARENT;
    }
}
