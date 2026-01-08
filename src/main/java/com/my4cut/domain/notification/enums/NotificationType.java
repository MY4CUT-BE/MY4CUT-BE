package com.my4cut.domain.notification.enums;

/**
 * 알림의 유형을 정의하는 열거형이다.
 * 친구 요청, 워크스페이스 초대, 미디어 댓글 등 다양한 알림 상황을 구분한다.
 */
public enum NotificationType {
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,
    WORKSPACE_INVITE,
    MEDIA_UPLOADED,
    MEDIA_COMMENT
}
