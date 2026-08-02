package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedTagNameException extends RuntimeException {
    private final ErrorCode errorCode;

    public DuplicatedTagNameException(String name) {
        super(TagErrorCode.DUPLICATED_NAME.message(name));
        this.errorCode = TagErrorCode.DUPLICATED_NAME;
    }
}
