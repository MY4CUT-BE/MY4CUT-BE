package com.my4cut.global.response;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 성공 코드를 관리하는 Enum 클래스.
 *
 * @author koohyunmo
 * @since 2026-01-07
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {

    // Common Success
    OK(HttpStatus.OK, "C2001", "요청에 성공하였습니다."),
    CREATED(HttpStatus.CREATED, "C2011", "리소스 생성에 성공하였습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "C2041", "요청은 성공했으나 응답 본문에 보낼 데이터가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
