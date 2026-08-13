package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class PostNotFoundException extends BusinessException {

    public PostNotFoundException(UUID id) {
        super(PostErrorCode.NOT_FOUND, id);
    }

    public PostNotFoundException(String slug) {
        super(PostErrorCode.NOT_FOUND, slug);
    }
}
