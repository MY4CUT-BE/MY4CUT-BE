package com.my4cut.domain.notification.exception;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseCode {

    // 알림 관련 에러
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N4041", "알림을 찾을 수 없습니다."
    ),

    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "N4042", "사용자를 찾을 수 없습니다."
    ),

    // 권한 에러
    NOT_NOTIFICATION_OWNER(HttpStatus.FORBIDDEN, "N4031", "본인의 알림만 처리할 수 있습니다."
    ),

    // 요청 값 오류
    INVALID_DEVICE_TYPE(HttpStatus.BAD_REQUEST, "N4001", "유효하지 않은 디바이스 타입입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
