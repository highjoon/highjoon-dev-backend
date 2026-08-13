package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class CategoryReferenceNotFoundException extends BusinessException {

    public CategoryReferenceNotFoundException(UUID id) {
        super(CategoryErrorCode.REFERENCE_NOT_FOUND, id);
    }
}
