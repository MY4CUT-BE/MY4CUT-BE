package com.my4cut.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    public void sendPush(String fcmToken, String title, String body, String type, Long targetId) {
        log.info("[FCM] 발송 로직 진입 - type={}, targetId={}, tokenExists={}",
                type, targetId, fcmToken != null);

        if (!firebaseEnabled) {
            log.info("[FCM] 발송 중단 - Firebase disabled");
            return;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[FCM] 발송 중단 - FCM token 없음");
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", type == null ? "" : type)
                .putData("targetId", targetId == null ? "" : String.valueOf(targetId))
                .build();

        log.info("[FCM] payload 생성 완료 - title={}, body={}, type={}, targetId={}, token={}",
                title, body, type, targetId, maskToken(fcmToken));

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 발송 성공 - response={}", response);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 발송 실패 - errorCode={}, messagingErrorCode={}, message={}",
                    e.getErrorCode(),
                    e.getMessagingErrorCode(),
                    e.getMessage(),
                    e);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "null";
        return token.substring(0, 8) + "...";
    }
}
