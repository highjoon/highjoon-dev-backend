package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(UUID id) {
        super(CategoryErrorCode.NOT_FOUND, id);
    }

    public CategoryNotFoundException(String slug) {
        super(CategoryErrorCode.NOT_FOUND, slug);
    }
}
