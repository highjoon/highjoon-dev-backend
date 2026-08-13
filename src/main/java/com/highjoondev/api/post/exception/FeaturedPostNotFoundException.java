package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.BusinessException;

public class FeaturedPostNotFoundException extends BusinessException {

    public FeaturedPostNotFoundException() {
        super(PostErrorCode.FEATURED_NOT_FOUND);
    }
}
