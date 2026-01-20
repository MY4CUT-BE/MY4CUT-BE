package com.my4cut.global.exception;

import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리를 담당하는 핸들러 클래스.
 *
 * @author koohyunmo
 * @since 2026-01-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Produce an error ApiResponse based on the thrown BusinessException's ErrorCode.
     *
     * @param e the thrown BusinessException whose ErrorCode determines the HTTP status and response body
     * @return a ResponseEntity containing an ApiResponse failure built from the exception's ErrorCode with the corresponding HTTP status
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode));
    }

    /**
     * Handle any uncaught exception and produce a standardized internal server error response.
     *
     * Logs the exception message and stack trace, and returns an ApiResponse containing
     * ErrorCode.INTERNAL_SERVER_ERROR with the corresponding HTTP status.
     *
     * @param e the exception that was thrown
     * @return a ResponseEntity containing an ApiResponse<Void> representing an internal server error
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}