package com.my4cut.domain.friend.event;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendAcceptedNotificationListener {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FriendAcceptedEvent event) {
        try {
            User requester = userRepository.findById(event.requesterId())
                    .orElseThrow(() -> new IllegalStateException(
                            "친구 요청자를 찾을 수 없습니다. userId=" + event.requesterId()));
            User accepter = userRepository.findById(event.accepterId())
                    .orElseThrow(() -> new IllegalStateException(
                            "친구 수락자를 찾을 수 없습니다. userId=" + event.accepterId()));

            notificationService.sendFriendAcceptedNotification(requester, accepter);
        } catch (RuntimeException exception) {
            log.error(
                    "친구 수락 알림 발송 실패 - requesterId={}, accepterId={}",
                    event.requesterId(),
                    event.accepterId(),
                    exception
            );
        }
    }
}
