package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import java.util.UUID;
import lombok.Getter;

@Getter
public class CategoryInvalidParentException extends RuntimeException {
    private final ErrorCode errorCode;

    public CategoryInvalidParentException(UUID id) {
        super(CategoryErrorCode.INVALID_PARENT.message(id));
        this.errorCode = CategoryErrorCode.INVALID_PARENT;
    }
}
