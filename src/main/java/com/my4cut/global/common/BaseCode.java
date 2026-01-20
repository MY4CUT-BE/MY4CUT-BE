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
 * Get the HTTP status associated with this code.
 *
 * @return the HTTP status corresponding to this code
 */
    HttpStatus getStatus();

    /**
 * Response code identifier.
 *
 * @return the response code (for example, "C2000" or "C4001")
 */
    String getCode();

    /**
 * Response message associated with this code.
 *
 * @return the response message
 */
    String getMessage();
}