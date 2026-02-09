package com.my4cut.domain.notification.dto.res;

import com.my4cut.domain.notification.entity.Notification;
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
            return switch (notification.getType()) {
                case FRIEND_REQUEST ->
                        senderNickname + "님의 친구 요청";
                case FRIEND_ACCEPTED ->
                        senderNickname + "님이 친구 요청을 수락했습니다.";
                case WORKSPACE_INVITE ->
                        senderNickname + "님이 " + workspaceName + "스페이스에 초대했습니다.";
                case MEDIA_COMMENT ->
                        senderNickname + "님이 " + workspaceName + "스페이스에 댓글을 남겼습니다.";
                case MEDIA_UPLOADED ->
                        senderNickname + "님이 새로운 미디어를 업로드했습니다.";
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