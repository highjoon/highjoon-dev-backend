package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PostNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public PostNotFoundException(UUID id) {
        super(PostErrorCode.NOT_FOUND.message(id));
        this.errorCode = PostErrorCode.NOT_FOUND;
    }

    public PostNotFoundException(String slug) {
        super(PostErrorCode.NOT_FOUND.message(slug));
        this.errorCode = PostErrorCode.NOT_FOUND;
    }
}
