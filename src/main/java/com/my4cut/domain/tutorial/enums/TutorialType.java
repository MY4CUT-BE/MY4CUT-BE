package com.my4cut.domain.tutorial.enums;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;

import java.util.Locale;

public enum TutorialType {
    HOME,
    UPLOAD_DATE,
    UPLOAD_CONTENT,
    RETOUCH_MAIN,
    RETOUCH_SPACE,
    RETOUCH_PHOTO,
    RETOUCH_DETAIL;

    public static TutorialType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        try {
            return TutorialType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
