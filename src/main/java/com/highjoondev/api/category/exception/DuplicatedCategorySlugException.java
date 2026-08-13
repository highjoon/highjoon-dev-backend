package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.BusinessException;

public class DuplicatedCategorySlugException extends BusinessException {

    public DuplicatedCategorySlugException(String slug) {
        super(CategoryErrorCode.DUPLICATED_SLUG, slug);
    }
}
