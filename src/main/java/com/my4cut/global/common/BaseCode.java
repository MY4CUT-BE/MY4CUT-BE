package com.my4cut.global.common;

import org.springframework.http.HttpStatus;

/**
 * 성공 및 에러 코드의 공통 인터페이스.
 *
 * @author koohyunmo
 * @since 2026-01-07
 */
public interface BaseCode {

    /**
     * HTTP 상태 코드를 반환합니다.
     *
     * @return HttpStatus 객체
     */
    HttpStatus getStatus();

    /**
     * 응답 코드를 반환합니다.
     *
     * @return 응답 코드 (예: C2000, C4001)
     */
    String getCode();

    /**
     * 응답 메시지를 반환합니다.
     *
     * @return 응답 메시지
     */
    String getMessage();
}