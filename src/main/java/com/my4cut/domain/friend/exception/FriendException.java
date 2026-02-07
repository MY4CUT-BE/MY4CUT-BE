package com.my4cut.domain.friend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FriendException extends RuntimeException {
    private final FriendErrorCode errorCode;
}
