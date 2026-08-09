package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class FeaturedPostNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public FeaturedPostNotFoundException() {
        super(PostErrorCode.FEATURED_NOT_FOUND.message());
        this.errorCode = PostErrorCode.FEATURED_NOT_FOUND;
    }
}
