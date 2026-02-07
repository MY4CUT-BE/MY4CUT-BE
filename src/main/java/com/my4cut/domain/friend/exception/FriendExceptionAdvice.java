package com.my4cut.domain.friend.exception;

import com.my4cut.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class FriendExceptionAdvice {

    @ExceptionHandler(FriendException.class)
    protected ResponseEntity<ApiResponse<Void>> handleFriendException(FriendException e) {
        log.error("FriendException: {}", e.getErrorCode().getMessage());
        FriendErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode));
    }
}
