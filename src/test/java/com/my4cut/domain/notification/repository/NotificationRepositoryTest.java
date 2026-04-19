package com.my4cut.domain.notification.repository;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("created_at 기준 cutoff 이전 알림만 배치 삭제한다")
    void deleteBatchByCreatedAtBefore_deletesOnlyNotificationsOlderThanCutoff() {
        User user = User.builder()
                .email("notification-cleanup@test.com")
                .nickname("cleanup")
                .loginType(LoginType.EMAIL)
                .friendCode("CLEAN001")
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);

        LocalDateTime now = LocalDateTime.of(2026, 4, 19, 9, 0);
        LocalDateTime cutoff = now.minusDays(30);

        Notification oldNotification = persistNotification(user);
        Notification boundaryNotification = persistNotification(user);
        Notification recentNotification = persistNotification(user);

        em.flush();
        updateCreatedAt(oldNotification.getId(), now.minusDays(31));
        updateCreatedAt(boundaryNotification.getId(), cutoff);
        updateCreatedAt(recentNotification.getId(), now.minusDays(29));
        em.flush();
        em.clear();

        int deletedCount = notificationRepository.deleteBatchByCreatedAtBefore(cutoff, 100);
        em.flush();
        em.clear();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(notificationRepository.existsById(oldNotification.getId())).isFalse();
        assertThat(notificationRepository.existsById(boundaryNotification.getId())).isTrue();
        assertThat(notificationRepository.existsById(recentNotification.getId())).isTrue();
    }

    private Notification persistNotification(User user) {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.FRIEND_REQUEST)
                .referenceId(1L)
                .isRead(false)
                .build();

        em.persist(notification);
        return notification;
    }

    private void updateCreatedAt(Long notificationId, LocalDateTime createdAt) {
        // @CreatedDate가 persist 시점에 값을 채우므로, 테스트 데이터만 DB 값을 직접 고정한다.
        em.createNativeQuery("update notifications set created_at = :createdAt where id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", notificationId)
                .executeUpdate();
    }
}
