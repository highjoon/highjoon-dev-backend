package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedPostSlugException extends RuntimeException {
    private final ErrorCode errorCode;

    public DuplicatedPostSlugException(String slug) {
        super(PostErrorCode.DUPLICATED_SLUG.message(slug));
        this.errorCode = PostErrorCode.DUPLICATED_SLUG;
    }
}
