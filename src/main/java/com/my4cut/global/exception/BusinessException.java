package com.my4cut.global.exception;

import com.my4cut.global.response.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직 실행 중 발생하는 전용 예외 클래스.
 *
 * @author koohyunmo
 * @since 2026-01-07
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Creates a BusinessException that represents the specified business error.
     *
     * The exception's message is initialized from the provided ErrorCode's message.
     *
     * @param errorCode the ErrorCode representing the business error
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}