package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedCategorySlugException extends RuntimeException {
    private final ErrorCode errorCode;

    public DuplicatedCategorySlugException(String slug) {
        super(CategoryErrorCode.DUPLICATED_SLUG.message(slug));
        this.errorCode = CategoryErrorCode.DUPLICATED_SLUG;
    }
}
