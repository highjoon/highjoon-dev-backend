package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import java.util.UUID;
import lombok.Getter;

@Getter
public class DuplicatedFeaturedPostException extends RuntimeException {
    private final ErrorCode errorCode;

    public DuplicatedFeaturedPostException(UUID id) {
        super(PostErrorCode.DUPLICATED_FEATURED_POST.message(id));
        this.errorCode = PostErrorCode.DUPLICATED_FEATURED_POST;
    }
}
