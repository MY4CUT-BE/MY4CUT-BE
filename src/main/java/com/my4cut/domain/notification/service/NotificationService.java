package com.my4cut.domain.notification.service;

import com.my4cut.domain.notification.dto.req.NotificationReqDto;
import com.my4cut.domain.notification.dto.res.NotificationResDto;
import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;
import com.my4cut.domain.notification.exception.NotificationErrorCode;
import com.my4cut.domain.notification.exception.NotificationException;
import com.my4cut.domain.notification.repository.NotificationRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    // FCM 토큰 등록
    @Transactional
    public NotificationResDto.RegisterTokenResDto registerFcmToken(
            Long userId,
            NotificationReqDto.RegisterTokenDto request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        DeviceType deviceType = DeviceType.valueOf(request.device().toUpperCase());

        UserFcmToken existingToken = userFcmTokenRepository
                .findByUserAndDeviceType(user, deviceType)
                .orElse(null);

        if (existingToken != null) {
            existingToken.updateToken(request.fcmToken());
            return NotificationResDto.RegisterTokenResDto.of(existingToken.getId());
        }

        UserFcmToken fcmToken = UserFcmToken.builder()
                .user(user)
                .fcmToken(request.fcmToken())
                .deviceType(deviceType)
                .build();

        UserFcmToken savedToken = userFcmTokenRepository.save(fcmToken);

        return NotificationResDto.RegisterTokenResDto.of(savedToken.getId());
    }

    // 친구요청을 보냈을 때 요청 받은 사용자에게 알림을 보냅니다.
    @Transactional
    public void sendFriendRequestNotification(
            User toUser,
            Long friendRequestId
    ) {
        Notification notification = Notification.builder()
                .user(toUser)
                .type(NotificationType.FRIEND_REQUEST)
                .referenceId(friendRequestId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * 워크스페이스 초대를 받았을 때 초대 받은 사용자에게 알림을 보냅니다.
     * @param toUser 알림을 받을 사용자
     * @param fromUser 초대를 보낸 사용자
     * @param workspace 초대된 워크스페이스
     * @param invitationId 생성된 초대장의 ID
     */
    @Transactional
    public void sendWorkspaceInviteNotification(
            User toUser,
            User fromUser,
            Workspace workspace,
            Long invitationId
    ) {
        Notification notification = Notification.builder()
                .user(toUser)
                .type(NotificationType.WORKSPACE_INVITE)
                .senderId(fromUser.getId())
                .workspaceId(workspace.getId())
                .referenceId(invitationId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDto.NotificationItemDto> getNotifications(
            Long userId,
            int page
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, 20);

        Page<Notification> notifications =
                notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return notifications.stream()
                .map(notification -> {
                    String senderNickname = null;
                    String senderProfileImageUrl = null;
                    String workspaceName = null;

                    if (notification.getSenderId() != null) {
                        senderNickname = userRepository.findById(notification.getSenderId())
                                .map(User::getNickname)
                                .orElse("알 수 없음"); //알림은 유효하지만 부가 정보 따로 없음.
                    }

                    if (notification.getWorkspaceId() != null) {
                        workspaceName = workspaceRepository.findById(notification.getWorkspaceId())
                                .map(Workspace::getName)
                                .orElse("알 수 없음");
                    }

                    return NotificationResDto.NotificationItemDto.of(
                            notification,
                            senderNickname,
                            senderProfileImageUrl,
                            workspaceName
                    );
                })
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public NotificationResDto.ReadNotificationResDto markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOT_NOTIFICATION_OWNER);
        }
        // 읽음 처리
        notification.markAsRead();

        return NotificationResDto.ReadNotificationResDto.of(notification);
    }

    // 친구요청을 보냈을 때 요청 받은 사용자에게 알림을 보냅니다.
    @Transactional
    public void sendFriendRequestNotification(
            User toUser,
            User fromUser,
            Long friendRequestId
    ) {
        Notification notification = Notification.builder()
                .user(toUser) //알림 받는 사람
                .type(NotificationType.FRIEND_REQUEST)
                .senderId(fromUser.getId()) //알림 보낸 사람
                .referenceId(friendRequestId) //친구요청 id
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    //친구수락을 했을 때
    @Transactional
    public void sendFriendAcceptedNotification(
            User toUser,        // 친구 요청 보낸 사람
            User fromUser       // 수락한 사람
    ) {
        Notification notification = Notification.builder()
                .user(toUser)
                .type(NotificationType.FRIEND_ACCEPTED)
                .senderId(fromUser.getId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // 워크스페이스 초대 알림 생성
    @Transactional
    public void sendWorkspaceInviteNotification(
            User invitee,           // 초대받은 사람
            User inviter,           // 초대한 사람
            Long workspaceId,
            Long invitationId       // 수락/거절용
    ) {
        Notification notification = Notification.builder()
                .user(invitee)
                .type(NotificationType.WORKSPACE_INVITE)
                .senderId(inviter.getId())
                .workspaceId(workspaceId)
                .referenceId(invitationId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // 댓글 알림 생성
    @Transactional
    public void sendMediaCommentNotification(
            User owner,         // 미디어 주인
            User commenter,     // 댓글 단 사람
            Long workspaceId,
            Long commentId
    ) {
        Notification notification = Notification.builder()
                .user(owner)
                .type(NotificationType.MEDIA_COMMENT)
                .senderId(commenter.getId())
                .workspaceId(workspaceId)
                .referenceId(commentId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // 미디어 업로드 알림
    @Transactional
    public void sendMediaUploadedNotification(
            User targetUser,    // 알림 받을 사람
            User uploader,      // 업로드한 사람
            Long workspaceId,
            Long mediaId
    ) {
        Notification notification = Notification.builder()
                .user(targetUser)
                .type(NotificationType.MEDIA_UPLOADED)
                .senderId(uploader.getId())
                .workspaceId(workspaceId)
                .referenceId(mediaId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}