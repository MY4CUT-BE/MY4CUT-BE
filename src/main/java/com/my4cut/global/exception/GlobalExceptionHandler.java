package com.my4cut.global.exception;

import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
     * BusinessException 처리 메소드.
     *
     * @param e 발생한 BusinessException
     * @return 에러 응답 객체 (ResponseEntity)
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
     * JSON 파싱 오류 처리 메소드 (잘못된 enum 값, 타입 불일치 등).
     *
     * @param e 발생한 HttpMessageNotReadableException
     * @return 에러 응답 객체 (ResponseEntity)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.BAD_REQUEST));
    }

    /**
     * 그 외 모든 Exception 처리 메소드.
     *
     * @param e 발생한 Exception
     * @return 에러 응답 객체 (ResponseEntity)
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}