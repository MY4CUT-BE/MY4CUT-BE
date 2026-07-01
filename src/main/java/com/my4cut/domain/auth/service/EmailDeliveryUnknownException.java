package com.my4cut.domain.auth.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;

public class EmailDeliveryUnknownException extends BusinessException {

    public EmailDeliveryUnknownException(Throwable cause) {
        super(ErrorCode.AUTH_EMAIL_DELIVERY_UNKNOWN, cause);
    }
}
