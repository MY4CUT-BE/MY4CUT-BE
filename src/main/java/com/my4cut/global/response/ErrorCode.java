package com.my4cut.global.response;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러 코드를 관리하는 Enum 클래스.
 *
 * @author koohyunmo
 * @since 2026-01-07
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {

    // Common Error (4xx)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "C4001", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C4011", "인증되지 않은 사용자입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C4031", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C4041", "요청한 리소스를 찾을 수 없습니다."),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "C4081", "요청 시간이 초과되었습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "C4291", "너무 많은 요청을 보냈습니다."),

    // Common Error (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C5001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}