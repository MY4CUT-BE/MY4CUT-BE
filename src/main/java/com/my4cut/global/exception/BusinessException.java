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
     * BusinessException 생성자.
     *
     * @param errorCode 발생한 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
