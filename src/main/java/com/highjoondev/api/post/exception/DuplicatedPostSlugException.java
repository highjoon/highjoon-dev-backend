package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.BusinessException;

public class DuplicatedPostSlugException extends BusinessException {

    public DuplicatedPostSlugException(String slug) {
        super(PostErrorCode.DUPLICATED_SLUG, slug);
    }
}
