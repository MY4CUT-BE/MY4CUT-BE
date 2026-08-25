package com.my4cut.domain.notification.service;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.repository.NotificationRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private UserFcmTokenRepository userFcmTokenRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private FcmService fcmService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                userFcmTokenRepository,
                notificationRepository,
                userRepository,
                workspaceRepository,
                fcmService
        );
        ReflectionTestUtils.setField(notificationService, "firebaseEnabled", true);
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 100L);
            return notification;
        });
    }

    @Test
    void friendRequest_savesNotificationAndSendsPush() {
        User receiver = user(1L, "receiver");
        User sender = user(2L, "sender");
        UserFcmToken token = token(receiver, "token");
        given(userFcmTokenRepository.findAllByUser(receiver)).willReturn(List.of(token));
        given(fcmService.sendPush("token", DeviceType.ANDROID, "친구 요청", "sender님이 회원님에게 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST", 100L, 10L, null, null)).willReturn(FcmSendResult.SUCCESS);

        notificationService.sendFriendRequestNotification(receiver, sender, 10L);

        verify(notificationRepository).save(any(Notification.class));
        verify(fcmService).sendPush("token", DeviceType.ANDROID, "친구 요청", "sender님이 회원님에게 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST", 100L, 10L, null, null);
    }

    @Test
    void friendAccepted_savesNotificationAndSendsPush() {
        User receiver = user(1L, "receiver");
        User sender = user(2L, "sender");
        given(userFcmTokenRepository.findAllByUser(receiver)).willReturn(List.of(token(receiver, "token")));
        given(fcmService.sendPush("token", DeviceType.ANDROID, "친구 요청 수락", "sender님이 친구 초대를 수락하였습니다.",
                "FRIEND_ACCEPTED", 100L, null, null, null)).willReturn(FcmSendResult.SUCCESS);

        notificationService.sendFriendAcceptedNotification(receiver, sender);

        verify(fcmService).sendPush("token", DeviceType.ANDROID, "친구 요청 수락", "sender님이 친구 초대를 수락하였습니다.",
                "FRIEND_ACCEPTED", 100L, null, null, null);
    }

    @Test
    void workspaceInvite_savesNotificationAndSendsPush() {
        User receiver = user(1L, "receiver");
        User sender = user(2L, "sender");
        Workspace workspace = Workspace.builder().name("space").creator(sender).build();
        ReflectionTestUtils.setField(workspace, "id", 20L);
        given(userFcmTokenRepository.findAllByUser(receiver)).willReturn(List.of(token(receiver, "token")));
        given(fcmService.sendPush("token", DeviceType.ANDROID, "워크스페이스 초대", "sender님이 space 스페이스에 회원님을 초대했습니다.",
                "WORKSPACE_INVITE", 100L, 30L, 20L, null)).willReturn(FcmSendResult.SUCCESS);

        notificationService.sendWorkspaceInviteNotification(receiver, sender, workspace, 30L);

        verify(fcmService).sendPush("token", DeviceType.ANDROID, "워크스페이스 초대", "sender님이 space 스페이스에 회원님을 초대했습니다.",
                "WORKSPACE_INVITE", 100L, 30L, 20L, null);
    }

    @Test
    void invalidToken_isDeletedAfterFailedSend() {
        User receiver = user(1L, "receiver");
        User sender = user(2L, "sender");
        UserFcmToken token = token(receiver, "expired-token");
        given(userFcmTokenRepository.findAllByUser(receiver)).willReturn(List.of(token));
        given(fcmService.sendPush("expired-token", DeviceType.ANDROID, "친구 요청", "sender님이 회원님에게 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST", 100L, 10L, null, null)).willReturn(FcmSendResult.INVALID_TOKEN);

        notificationService.sendFriendRequestNotification(receiver, sender, 10L);

        verify(userFcmTokenRepository).delete(token);
    }

    @Test
    void pushUsesEachRegisteredTokenDeviceType() {
        User receiver = user(1L, "receiver");
        User sender = user(2L, "sender");
        UserFcmToken androidToken = token(receiver, "android-token", DeviceType.ANDROID);
        UserFcmToken iosToken = token(receiver, "ios-token", DeviceType.IOS);
        given(userFcmTokenRepository.findAllByUser(receiver))
                .willReturn(List.of(androidToken, iosToken));

        notificationService.sendFriendRequestNotification(receiver, sender, 10L);

        verify(fcmService).sendPush("android-token", DeviceType.ANDROID,
                "친구 요청", "sender님이 회원님에게 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST", 100L, 10L, null, null);
        verify(fcmService).sendPush("ios-token", DeviceType.IOS,
                "친구 요청", "sender님이 회원님에게 친구 요청을 보냈습니다.",
                "FRIEND_REQUEST", 100L, 10L, null, null);
    }

    private User user(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserFcmToken token(User user, String value) {
        return token(user, value, DeviceType.ANDROID);
    }

    private UserFcmToken token(User user, String value, DeviceType deviceType) {
        UserFcmToken token = UserFcmToken.builder()
                .user(user)
                .fcmToken(value)
                .deviceType(deviceType)
                .build();
        ReflectionTestUtils.setField(token, "id", 50L);
        return token;
    }
}
