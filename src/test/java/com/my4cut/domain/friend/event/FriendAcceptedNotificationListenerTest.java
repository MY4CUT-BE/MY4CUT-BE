package com.my4cut.domain.friend.event;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendAcceptedNotificationListenerTest {

    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @Test
    void handlesAcceptedEventAfterCommitAsynchronously() throws Exception {
        Method method = FriendAcceptedNotificationListener.class
                .getMethod("handle", FriendAcceptedEvent.class);

        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void sendsAcceptedNotificationForEventUsers() {
        User requester = user(1L, "requester");
        User accepter = user(2L, "accepter");
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(accepter));
        FriendAcceptedNotificationListener listener =
                new FriendAcceptedNotificationListener(userRepository, notificationService);

        listener.handle(new FriendAcceptedEvent(1L, 2L));

        verify(notificationService).sendFriendAcceptedNotification(requester, accepter);
    }

    @Test
    void notificationFailureDoesNotEscapeAsyncListener() {
        User requester = user(1L, "requester");
        User accepter = user(2L, "accepter");
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(accepter));
        willThrow(new IllegalStateException("FCM unavailable"))
                .given(notificationService)
                .sendFriendAcceptedNotification(requester, accepter);
        FriendAcceptedNotificationListener listener =
                new FriendAcceptedNotificationListener(userRepository, notificationService);

        assertThatCode(() -> listener.handle(new FriendAcceptedEvent(1L, 2L)))
                .doesNotThrowAnyException();
    }

    private User user(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
