package com.highjoondev.api.category.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedCategorySlugException extends RuntimeException {
    private final ErrorCode errorCode;

    public DuplicatedCategorySlugException(String slug) {
        super(ErrorCode.DUPLICATED_SLUG.message(slug));
        this.errorCode = ErrorCode.DUPLICATED_SLUG;
    }
}
