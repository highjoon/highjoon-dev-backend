package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.ErrorCode;
import java.util.UUID;
import lombok.Getter;

@Getter
public class TagNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public TagNotFoundException(UUID id) {
        super(TagErrorCode.NOT_FOUND.message(id));
        this.errorCode = TagErrorCode.NOT_FOUND;
    }

    public TagNotFoundException(String name) {
        super(TagErrorCode.NOT_FOUND.message(name));
        this.errorCode = TagErrorCode.NOT_FOUND;
    }
}
