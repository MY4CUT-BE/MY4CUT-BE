// NotificationResDto.java
package com.my4cut.domain.notification.dto.res;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;

public record NotificationResDto() {

    // FCM 토큰 등록 응답
    public record RegisterTokenResDto(
            Long id
    ) {
        public static RegisterTokenResDto of(Long id) {
            return new RegisterTokenResDto(id);
        }
    }

    // 알림 목록 조회 응답
    public record NotificationItemDto(
            Long id,
            NotificationType type,
            String msg,
            Boolean isRead
    ) {
        public static NotificationItemDto of(Notification notification) {
            return new NotificationItemDto(
                    notification.getId(),
                    notification.getType(),
                    generateMessage(notification),
                    notification.getIsRead()
            );
        }

        private static String generateMessage(Notification notification) {
            return switch (notification.getType()) {
                case FRIEND_REQUEST -> "친구 요청이 도착했습니다.";
                case FRIEND_ACCEPTED -> "친구 요청이 수락되었습니다.";
                case WORKSPACE_INVITE -> "워크스페이스에 초대되었습니다.";
                case MEDIA_UPLOADED -> "새로운 미디어가 업로드되었습니다.";
                case MEDIA_COMMENT -> "미디어에 댓글이 달렸습니다.";
            };
        }
    }

    // 알림 읽음 처리 응답
    public record ReadNotificationResDto(
            Boolean isRead
    ) {
        public static ReadNotificationResDto of(Notification notification) {
            return new ReadNotificationResDto(notification.getIsRead());
        }
    }
}