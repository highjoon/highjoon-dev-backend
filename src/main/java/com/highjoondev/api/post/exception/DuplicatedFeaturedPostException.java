package com.highjoondev.api.post.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class DuplicatedFeaturedPostException extends BusinessException {

    public DuplicatedFeaturedPostException(UUID id) {
        super(PostErrorCode.DUPLICATED_FEATURED_POST, id);
    }
}
