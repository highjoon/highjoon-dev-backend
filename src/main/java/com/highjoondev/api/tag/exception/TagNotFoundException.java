package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.UUID;

public class TagNotFoundException extends BusinessException {

    public TagNotFoundException(UUID id) {
        super(TagErrorCode.NOT_FOUND, id);
    }

    public TagNotFoundException(String name) {
        super(TagErrorCode.NOT_FOUND, name);
    }
}
