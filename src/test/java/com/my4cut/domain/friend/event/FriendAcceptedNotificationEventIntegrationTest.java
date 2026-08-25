package com.my4cut.domain.friend.event;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(classes = {
        FriendAcceptedNotificationListener.class,
        FriendAcceptedNotificationEventIntegrationTest.TestConfig.class
})
class FriendAcceptedNotificationEventIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private NotificationService notificationService;

    private User requester;
    private User accepter;

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @BeforeEach
    void setUp() {
        requester = user(1L, "requester");
        accepter = user(2L, "accepter");
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(accepter));
    }

    @Test
    void sendsNotificationOnlyAfterTransactionCommit() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new FriendAcceptedEvent(1L, 2L));
            verifyNoInteractions(notificationService);
        });

        verify(notificationService, after(2_000))
                .sendFriendAcceptedNotification(requester, accepter);
    }

    @Test
    void doesNotSendNotificationWhenTransactionRollsBack() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new FriendAcceptedEvent(1L, 2L));
            status.setRollbackOnly();
        });

        verify(notificationService, after(500).never())
                .sendFriendAcceptedNotification(requester, accepter);
    }

    private User user(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
