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
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C5001", "서버 내부 오류가 발생했습니다."),

    // User Domain Error
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U4041", "사용자를 찾을 수 없습니다."),
    USER_DELETED(HttpStatus.UNAUTHORIZED, "U4011", "탈퇴한 사용자입니다."),

    // Auth Domain Error
    AUTH_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A4091", "이미 가입된 이메일입니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A4011", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A4012", "유효하지 않은 리프레시 토큰입니다."),
    AUTH_INVALID_KAKAO_RESPONSE(HttpStatus.BAD_REQUEST, "A4001", "카카오 사용자 정보를 가져올 수 없습니다."),

    // Image Domain Error
    IMAGE_OWNER_NOT_FOUND(HttpStatus.NOT_FOUND, "I4041", "이미지 소유 사용자를 찾을 수 없습니다."),
    IMAGE_INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "I4001", "지원하지 않는 파일 이름 형식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}