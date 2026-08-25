package com.my4cut.domain.notification.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.my4cut.domain.user.enums.DeviceType;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmClient fcmClient;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    public FcmSendResult sendPush(String fcmToken,
                         DeviceType deviceType,
                         String title,
                         String body,
                         String type,
                         Long notificationId,
                         Long referenceId,
                         Long workspaceId,
                         Long mediaId) {
        log.info(
                "[FCM] 발송 로직 진입 - type={}, notificationId={}, referenceId={}, workspaceId={}, tokenExists={}",
                type,
                notificationId,
                referenceId,
                workspaceId,
                fcmToken != null
        );

        if (!firebaseEnabled) {
            log.info("[FCM] 발송 중단 - Firebase disabled");
            return FcmSendResult.SKIPPED;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[FCM] 발송 중단 - FCM token 없음");
            return FcmSendResult.SKIPPED;
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .putData("title", title == null ? "" : title)
                .putData("body", body == null ? "" : body)
                .putData("type", type == null ? "" : type)
                .putData("notificationId",
                        notificationId == null ? "" : String.valueOf(notificationId))
                .putData("referenceId",
                        referenceId == null ? "" : String.valueOf(referenceId))
                .putData("workspaceId",
                        workspaceId == null ? "" : String.valueOf(workspaceId))
                .putData("mediaId",
                        mediaId == null ? "" : String.valueOf(mediaId));

        if (deviceType == DeviceType.IOS) {
            messageBuilder
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-priority", "10")
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .build())
                            .build());
        }

        Message message = messageBuilder.build();

        log.info(
                "[FCM] payload 생성 완료 - title={}, body={}, type={}, notificationId={}, referenceId={}, workspaceId={}, token={}",
                title,
                body,
                type,
                notificationId,
                referenceId,
                workspaceId,
                maskToken(fcmToken)
        );

        try {
            String response = fcmClient.send(message);
            log.info("[FCM] 발송 성공 - response={}", response);
            return FcmSendResult.SUCCESS;
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 발송 실패 - errorCode={}, messagingErrorCode={}, message={}",
                    e.getErrorCode(),
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e);
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                return FcmSendResult.INVALID_TOKEN;
            }
            return FcmSendResult.FAILED;
        } catch (RuntimeException e) {
            log.error("[FCM] 예기치 않은 발송 실패 - message={}", e.getMessage(), e);
            return FcmSendResult.FAILED;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "null";
        return token.substring(0, 8) + "...";
    }
}
