package com.my4cut.domain.notification.exception;

import com.my4cut.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class NotificationExceptionAdvice {

    @ExceptionHandler(NotificationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNotificationException(NotificationException e) {
        log.error("NotificationException: {}", e.getErrorCode().getMessage());
        NotificationErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode));
    }
}
