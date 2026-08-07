package com.my4cut.domain.notification.repository;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;
import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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

    @Test
    @DisplayName("처리 완료된 친구 요청과 스페이스 초대 알림은 알림 목록에서 제외된다")
    void findVisibleByUserIdOrderByCreatedAtDesc_excludesProcessedActionNotifications() {
        User receiver = persistUser("visible-receiver@test.com", "visibleReceiver", "VISIBLE1");
        User sender = persistUser("visible-sender@test.com", "visibleSender", "VISIBLE2");

        FriendRequest pendingFriendRequest = persistFriendRequest(sender, receiver, FriendRequestStatus.PENDING);
        FriendRequest acceptedFriendRequest = persistFriendRequest(sender, receiver, FriendRequestStatus.ACCEPTED);
        WorkspaceInvitation pendingInvitation = persistWorkspaceInvitation(sender, receiver);
        WorkspaceInvitation acceptedInvitation = persistWorkspaceInvitation(sender, receiver);
        acceptedInvitation.accept();
        em.flush();

        Notification pendingFriendNotification = persistNotification(receiver, NotificationType.FRIEND_REQUEST, pendingFriendRequest.getId());
        persistNotification(receiver, NotificationType.FRIEND_REQUEST, acceptedFriendRequest.getId());
        Notification pendingWorkspaceNotification = persistNotification(receiver, NotificationType.WORKSPACE_INVITE, pendingInvitation.getId());
        persistNotification(receiver, NotificationType.WORKSPACE_INVITE, acceptedInvitation.getId());
        Notification normalNotification = persistNotification(receiver, NotificationType.FRIEND_ACCEPTED, null);
        em.flush();

        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 4, 19, 10, 0);
        updateCreatedAt(pendingFriendNotification.getId(), sameCreatedAt);
        updateCreatedAt(pendingWorkspaceNotification.getId(), sameCreatedAt);
        updateCreatedAt(normalNotification.getId(), sameCreatedAt);
        em.flush();
        em.clear();

        var notifications = notificationRepository
                .findVisibleByUserIdOrderByCreatedAtDesc(receiver.getId(), PageRequest.of(0, 10))
                .getContent();

        // 원본 요청이 PENDING인 액션 알림과 일반 알림만 조회되어야 한다.
        assertThat(notifications)
                .extracting(Notification::getId)
                .containsExactly(
                        normalNotification.getId(),
                        pendingWorkspaceNotification.getId(),
                        pendingFriendNotification.getId()
                );
        assertThat(notificationRepository.countVisibleUnreadByUserId(receiver.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("친구 요청 알림은 사용자, 타입, 참조 ID 기준으로 삭제된다")
    void deleteByUserAndTypeAndReferenceId_deletesTargetActionNotification() {
        User receiver = persistUser("delete-receiver@test.com", "deleteReceiver", "DELETE1");
        User otherReceiver = persistUser("delete-other@test.com", "deleteOther", "DELETE2");
        Notification notification = persistNotification(receiver, NotificationType.FRIEND_REQUEST, 10L);
        Notification otherUserNotification = persistNotification(otherReceiver, NotificationType.FRIEND_REQUEST, 10L);
        Notification otherTypeNotification = persistNotification(receiver, NotificationType.WORKSPACE_INVITE, 10L);
        Notification otherReferenceNotification = persistNotification(receiver, NotificationType.FRIEND_REQUEST, 11L);
        em.flush();
        em.clear();

        notificationRepository.deleteByUserAndTypeAndReferenceId(receiver, NotificationType.FRIEND_REQUEST, 10L);
        em.flush();
        em.clear();

        // 처리된 요청 알림만 물리 삭제해 알림창 재진입 시 같은 액션이 반복 표시되지 않게 한다.
        assertThat(notificationRepository.existsById(notification.getId())).isFalse();
        assertThat(notificationRepository.existsById(otherUserNotification.getId())).isTrue();
        assertThat(notificationRepository.existsById(otherTypeNotification.getId())).isTrue();
        assertThat(notificationRepository.existsById(otherReferenceNotification.getId())).isTrue();
    }

    private Notification persistNotification(User user) {
        return persistNotification(user, NotificationType.FRIEND_REQUEST, 1L);
    }

    private Notification persistNotification(User user, NotificationType type, Long referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        em.persist(notification);
        return notification;
    }

    private User persistUser(String email, String nickname, String friendCode) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);
        return user;
    }

    private FriendRequest persistFriendRequest(User fromUser, User toUser, FriendRequestStatus status) {
        FriendRequest request = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .status(status)
                .build();
        em.persist(request);
        return request;
    }

    private WorkspaceInvitation persistWorkspaceInvitation(User inviter, User invitee) {
        Workspace workspace = Workspace.builder()
                .name("visible-workspace")
                .creator(inviter)
                .build();
        em.persist(workspace);

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitee(invitee)
                .inviter(inviter)
                .build();
        em.persist(invitation);
        return invitation;
    }

    private void updateCreatedAt(Long notificationId, LocalDateTime createdAt) {
        // @CreatedDate가 persist 시점에 값을 채우므로, 테스트 데이터만 DB 값을 직접 고정한다.
        em.createNativeQuery("update notifications set created_at = :createdAt where id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", notificationId)
                .executeUpdate();
    }
}
