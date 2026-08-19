package com.highjoondev.api.tag.exception;

import com.highjoondev.api.global.exception.BusinessException;
import java.util.List;
import java.util.UUID;

public class TagReferenceNotFoundException extends BusinessException {

    public TagReferenceNotFoundException(List<UUID> ids) {
        super(TagErrorCode.REFERENCE_NOT_FOUND, ids);
    }
}
