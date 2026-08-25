package com.my4cut.domain.notification;

import com.my4cut.domain.notification.enums.NotificationType;

public final class NotificationMessage {

    private NotificationMessage() {
    }

    public static String format(
            NotificationType type,
            String senderNickname,
            String workspaceName
    ) {
        return switch (type) {
            case FRIEND_REQUEST ->
                    senderNickname + "님이 회원님에게 친구 요청을 보냈습니다.";
            case FRIEND_ACCEPTED ->
                    senderNickname + "님이 친구 초대를 수락하였습니다.";
            case WORKSPACE_INVITE ->
                    senderNickname + "님이 " + workspaceName + " 스페이스에 회원님을 초대했습니다.";
            case WORKSPACE_ACCEPTED ->
                    senderNickname + "님이 " + workspaceName + " 스페이스 초대를 수락했습니다.";
            case MEDIA_COMMENT ->
                    senderNickname + "님이 " + workspaceName + " 스페이스에 댓글을 남겼습니다.";
            case MEDIA_UPLOADED ->
                    senderNickname + "님이 " + workspaceName + " 스페이스에 사진을 업로드했습니다.";
        };
    }
}
