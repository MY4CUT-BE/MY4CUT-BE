package com.my4cut.domain.day4cut.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 하루네컷 도메인 예외 클래스.
 */
@Getter
@RequiredArgsConstructor
public class Day4CutException extends RuntimeException {

    private final Day4CutErrorCode errorCode;

    @Override
    public String getMessage() {
        return errorCode.getMessage();
    }
}
