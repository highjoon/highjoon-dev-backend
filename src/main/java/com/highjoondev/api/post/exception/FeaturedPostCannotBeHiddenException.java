package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class FeaturedPostCannotBeHiddenException extends RuntimeException {
    private final ErrorCode errorCode;

    public FeaturedPostCannotBeHiddenException() {
        super(PostErrorCode.FEATURED_CANNOT_BE_HIDDEN.message());
        this.errorCode = PostErrorCode.FEATURED_CANNOT_BE_HIDDEN;
    }
}
