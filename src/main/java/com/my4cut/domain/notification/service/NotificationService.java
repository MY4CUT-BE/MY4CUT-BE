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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FcmService fcmService;

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

    // 친구 요청 알림 생성 + FCM 발송
    @Transactional
    public void sendFriendRequestNotification(
            User toUser,
            User fromUser,
            Long friendRequestId
    ) {
        Notification notification = Notification.builder()
                .user(toUser)
                .type(NotificationType.FRIEND_REQUEST)
                .senderId(fromUser.getId())
                .referenceId(friendRequestId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        sendPushToUserTokens(
                toUser,
                "친구 요청",
                fromUser.getNickname() + "님이 친구 요청을 보냈습니다.",
                NotificationType.FRIEND_REQUEST.name(),
                friendRequestId
        );
    }

    // 친구 수락 알림 생성 + FCM 발송
    @Transactional
    public void sendFriendAcceptedNotification(
            User toUser,
            User fromUser
    ) {
        Notification notification = Notification.builder()
                .user(toUser)
                .type(NotificationType.FRIEND_ACCEPTED)
                .senderId(fromUser.getId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        sendPushToUserTokens(
                toUser,
                "친구 요청 수락",
                fromUser.getNickname() + "님이 친구 요청을 수락했습니다.",
                NotificationType.FRIEND_ACCEPTED.name(),
                null
        );
    }

    // 워크스페이스 초대 알림 생성 + FCM 발송
    @Transactional
    public void sendWorkspaceInviteNotification(
            User invitee,
            User inviter,
            Workspace workspace,
            Long invitationId
    ) {
        Notification notification = Notification.builder()
                .user(invitee)
                .type(NotificationType.WORKSPACE_INVITE)
                .senderId(inviter.getId())
                .workspaceId(workspace.getId())
                .referenceId(invitationId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        sendPushToUserTokens(
                invitee,
                "워크스페이스 초대",
                inviter.getNickname() + "님이 " + workspace.getName() + " 워크스페이스에 초대했습니다.",
                NotificationType.WORKSPACE_INVITE.name(),
                invitationId
        );
    }

    // 댓글 알림 생성 + FCM 발송
    @Transactional
    public void sendMediaCommentNotification(
            User owner,
            User commenter,
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

        sendPushToUserTokens(
                owner,
                "새 댓글",
                commenter.getNickname() + "님이 댓글을 남겼습니다.",
                NotificationType.MEDIA_COMMENT.name(),
                commentId
        );
    }

    // 미디어 업로드 알림 생성 + FCM 발송
    @Transactional
    public void sendMediaUploadedNotification(
            User targetUser,
            User uploader,
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

        sendPushToUserTokens(
                targetUser,
                "새 사진 업로드",
                uploader.getNickname() + "님이 사진을 업로드했습니다.",
                NotificationType.MEDIA_UPLOADED.name(),
                mediaId
        );
    }

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDto.NotificationItemDto> getNotifications(
            Long userId,
            int page
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, 8);

        Page<Notification> notifications =
                notificationRepository.findVisibleByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return notifications.stream()
                .map(notification -> {
                    String senderNickname = null;
                    String senderProfileImageUrl = null;
                    String workspaceName = null;

                    if (notification.getSenderId() != null) {
                        senderNickname = userRepository.findById(notification.getSenderId())
                                .map(User::getNickname)
                                .orElse("알 수 없음");
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

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOT_NOTIFICATION_OWNER);
        }

        notification.markAsRead();
        return NotificationResDto.ReadNotificationResDto.of(notification);
    }

    // 알림 개별 삭제
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOT_NOTIFICATION_OWNER);
        }

        notificationRepository.delete(notification);
    }

    // 친구 요청 알림 삭제
    @Transactional
    public void deleteFriendRequestNotification(User receiver, Long friendRequestId) {
        notificationRepository.deleteByUserAndTypeAndReferenceId(
                receiver,
                NotificationType.FRIEND_REQUEST,
                friendRequestId
        );
    }

    // 워크스페이스 초대 알림 삭제
    @Transactional
    public void deleteWorkspaceInviteNotification(User invitee, Long invitationId) {
        notificationRepository.deleteByUserAndTypeAndReferenceId(
                invitee,
                NotificationType.WORKSPACE_INVITE,
                invitationId
        );
    }

    // 알림 전체 삭제
    @Transactional
    public void deleteAllNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        notificationRepository.deleteAllByUser(user);
    }

    // 페이지 단위 읽음 처리
    @Transactional
    public void markPageAsRead(Long userId, NotificationReqDto.MarkReadByIdsDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        List<Notification> notifications =
                notificationRepository.findAllByIdInAndUser(request.notificationIds(), user);

        notifications.forEach(Notification::markAsRead);
    }

    // 읽지 않은 알림 여부 조회
    @Transactional(readOnly = true)
    public NotificationResDto.UnreadStatusResDto getUnreadStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        boolean hasUnread = notificationRepository.countVisibleUnreadByUserId(user.getId()) > 0;
        return NotificationResDto.UnreadStatusResDto.of(hasUnread);
    }

    private void sendPushToUserTokens(
            User user,
            String title,
            String body,
            String type,
            Long targetId
    ) {
        List<UserFcmToken> tokens = userFcmTokenRepository.findAllByUser(user);
        log.info("[FCM] 대상 userId={}, tokenCount={}", user.getId(), tokens.size());

        for (UserFcmToken token : tokens) {
            fcmService.sendPush(
                    token.getFcmToken(),
                    title,
                    body,
                    type,
                    targetId
            );
        }
    }
}