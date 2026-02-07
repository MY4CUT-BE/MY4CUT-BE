package com.my4cut.domain.friend.exception;

import com.my4cut.global.common.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FriendErrorCode implements BaseCode {

    // 친구 관련 에러
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "F4041", "친구를 찾을 수 없습니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "F4042", "친구 요청을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "F4043", "사용자를 찾을 수 없습니다."),

    // 권한 에러
    NOT_REQUEST_SENDER(HttpStatus.FORBIDDEN, "F4031", "해당 요청을 보낸 사람이 아닙니다."),
    NOT_REQUEST_RECEIVER(HttpStatus.FORBIDDEN, "F4032", "해당 요청을 받은 사람이 아닙니다."),

    // 비즈니스 로직 에러
    ALREADY_FRIEND(HttpStatus.BAD_REQUEST, "F4001", "이미 친구입니다."),
    DUPLICATE_FRIEND_REQUEST(HttpStatus.BAD_REQUEST, "F4002", "이미 친구 요청을 보냈습니다."),
    INVALID_REQUEST_STATUS(HttpStatus.BAD_REQUEST, "F4003", "처리할 수 없는 요청 상태입니다."),
    SELF_FRIEND_REQUEST(HttpStatus.BAD_REQUEST, "F4004", "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, "F4044", "친구 관계를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
