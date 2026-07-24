package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import java.util.UUID;
import lombok.Getter;

@Getter
public class CategoryParentNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public CategoryParentNotFoundException(UUID id) {
        super(CategoryErrorCode.PARENT_NOT_FOUND.message(id));
        this.errorCode = CategoryErrorCode.PARENT_NOT_FOUND;
    }
}
