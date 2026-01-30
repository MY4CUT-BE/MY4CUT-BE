package com.my4cut.domain.day4cut.exception;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 하루네컷 도메인 에러 코드를 관리하는 Enum 클래스.
 */
@Getter
@RequiredArgsConstructor
public enum Day4CutErrorCode implements BaseCode {

    // 4xx Client Error
    DAY4CUT_NOT_FOUND(HttpStatus.NOT_FOUND, "D4041", "해당 하루네컷을 찾을 수 없습니다."),
    DAY4CUT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "D4031", "해당 하루네컷에 대한 접근 권한이 없습니다."),
    DAY4CUT_INVALID_THUMBNAIL(HttpStatus.BAD_REQUEST, "D4001", "썸네일은 반드시 1개여야 합니다."),
    DAY4CUT_IMAGES_REQUIRED(HttpStatus.BAD_REQUEST, "D4002", "이미지는 최소 1장 이상이어야 합니다."),
    DAY4CUT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "D4003", "내용은 필수입니다."),
    DAY4CUT_INVALID_EMOJI_TYPE(HttpStatus.BAD_REQUEST, "D4004", "유효하지 않은 이모티콘 타입입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
