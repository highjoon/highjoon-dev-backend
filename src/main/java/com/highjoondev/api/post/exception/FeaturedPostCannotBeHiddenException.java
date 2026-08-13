package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.BusinessException;

public class FeaturedPostCannotBeHiddenException extends BusinessException {

    public FeaturedPostCannotBeHiddenException() {
        super(PostErrorCode.FEATURED_CANNOT_BE_HIDDEN);
    }
}
