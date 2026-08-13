package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class CategoryInvalidParentException extends BusinessException {

    public CategoryInvalidParentException(UUID id) {
        super(CategoryErrorCode.INVALID_PARENT, id);
    }
}
