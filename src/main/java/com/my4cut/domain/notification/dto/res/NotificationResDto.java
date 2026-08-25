package com.my4cut.domain.notification.dto.res;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.NotificationMessage;
import com.my4cut.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

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
            Long notificationId,     // 알림 id
            NotificationType type,
            String message,           // 최종 메시지
            Boolean isRead,
            Long referenceId,         // 친구요청 id (수락/거절용)
            Long mediaId,
            Long senderId,
            String senderNickname,
            String senderProfileImageUrl,
            Long workspaceId,
            String workspaceName,
            LocalDateTime createdAt
    ) {
        public static NotificationItemDto of(
                Notification notification,
                String senderNickname,
                String senderProfileImageUrl,
                String workspaceName
        ) {
            return new NotificationItemDto(
                    notification.getId(),
                    notification.getType(),
                    generateMessage(notification, senderNickname, workspaceName),
                    notification.getIsRead(),
                    notification.getReferenceId(),
                    notification.getMediaId(),
                    notification.getSenderId(),
                    senderNickname,
                    senderProfileImageUrl,
                    notification.getWorkspaceId(),
                    workspaceName,
                    notification.getCreatedAt()
            );
        }

        private static String generateMessage(
                Notification notification,
                String senderNickname,
                String workspaceName
        ) {
            return NotificationMessage.format(
                    notification.getType(),
                    senderNickname,
                    workspaceName
            );
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

    // 읽지 않은 알림 여부 조회 후 응답
    public record UnreadStatusResDto(
            Boolean hasUnread
    ) {
        public static UnreadStatusResDto of(boolean hasUnread) {
            return new UnreadStatusResDto(hasUnread);
        }
    }
}
