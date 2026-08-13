package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class CategoryParentNotFoundException extends BusinessException {

    public CategoryParentNotFoundException(UUID id) {
        super(CategoryErrorCode.PARENT_NOT_FOUND, id);
    }
}
