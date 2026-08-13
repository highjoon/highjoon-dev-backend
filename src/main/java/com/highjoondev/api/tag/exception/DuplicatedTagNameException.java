package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.BusinessException;

public class DuplicatedTagNameException extends BusinessException {

    public DuplicatedTagNameException(String name) {
        super(TagErrorCode.DUPLICATED_NAME, name);
    }
}
