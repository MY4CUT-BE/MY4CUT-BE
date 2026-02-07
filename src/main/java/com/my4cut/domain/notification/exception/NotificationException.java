package com.my4cut.domain.notification.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotificationException extends RuntimeException {
    private final NotificationErrorCode errorCode;
}
