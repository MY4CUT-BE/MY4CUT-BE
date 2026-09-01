package com.my4cut.domain.auth.service;

import com.my4cut.domain.friend.entity.Friend;
import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountWithdrawalCleanupIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserFcmTokenRepository userFcmTokenRepository;

    @Test
    @DisplayName("탈퇴 관계 정리는 활성 관계만 삭제하고 처리 완료된 요청과 콘텐츠 기반은 유지한다")
    void cleanup_DeletesOnlyActiveRelationships() {
        User withdrawingUser = persistUser("withdraw@test.com", "WITH01");
        User otherUser = persistUser("other@test.com", "OTHER1");
        Workspace workspace = Workspace.builder()
                .name("shared workspace")
                .creator(withdrawingUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        entityManager.persist(workspace);

        entityManager.persist(Friend.builder()
                .user(withdrawingUser)
                .friendUser(otherUser)
                .isFavorite(false)
                .build());
        entityManager.persist(Friend.builder()
                .user(otherUser)
                .friendUser(withdrawingUser)
                .isFavorite(false)
                .build());
        entityManager.persist(FriendRequest.builder()
                .fromUser(otherUser)
                .toUser(withdrawingUser)
                .status(FriendRequestStatus.PENDING)
                .build());
        entityManager.persist(FriendRequest.builder()
                .fromUser(withdrawingUser)
                .toUser(otherUser)
                .status(FriendRequestStatus.ACCEPTED)
                .build());
        entityManager.persist(WorkspaceMember.builder()
                .workspace(workspace)
                .user(withdrawingUser)
                .joinedAt(LocalDateTime.now())
                .build());
        entityManager.persist(WorkspaceMember.builder()
                .workspace(workspace)
                .user(otherUser)
                .joinedAt(LocalDateTime.now())
                .build());

        WorkspaceInvitation pendingInvitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(otherUser)
                .invitee(withdrawingUser)
                .build();
        entityManager.persist(pendingInvitation);
        WorkspaceInvitation acceptedInvitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(withdrawingUser)
                .invitee(otherUser)
                .build();
        acceptedInvitation.accept();
        entityManager.persist(acceptedInvitation);
        entityManager.persist(UserFcmToken.builder()
                .user(withdrawingUser)
                .fcmToken("withdraw-fcm-token")
                .deviceType(DeviceType.IOS)
                .build());
        entityManager.flush();

        AccountWithdrawalCleanupService cleanupService = new AccountWithdrawalCleanupService(
                friendRepository,
                friendRequestRepository,
                workspaceInvitationRepository,
                workspaceMemberRepository,
                userFcmTokenRepository
        );

        cleanupService.cleanup(withdrawingUser);
        entityManager.flush();
        entityManager.clear();

        assertThat(count("""
                select count(friend) from Friend friend
                where friend.user.id = :userId or friend.friendUser.id = :userId
                """, withdrawingUser.getId())).isZero();
        assertThat(count("""
                select count(request) from FriendRequest request
                where request.status = com.my4cut.domain.friend.enums.FriendRequestStatus.PENDING
                  and (request.fromUser.id = :userId or request.toUser.id = :userId)
                """, withdrawingUser.getId())).isZero();
        assertThat(count("select count(request) from FriendRequest request", withdrawingUser.getId()))
                .isEqualTo(1L);
        assertThat(count("""
                select count(invitation) from WorkspaceInvitation invitation
                where invitation.status = com.my4cut.domain.workspace.enums.InvitationStatus.PENDING
                  and (invitation.invitee.id = :userId or invitation.inviter.id = :userId)
                """, withdrawingUser.getId())).isZero();
        assertThat(count("select count(invitation) from WorkspaceInvitation invitation", withdrawingUser.getId()))
                .isEqualTo(1L);
        assertThat(count("""
                select count(member) from WorkspaceMember member
                where member.user.id = :userId
                """, withdrawingUser.getId())).isZero();
        assertThat(count("select count(member) from WorkspaceMember member", withdrawingUser.getId()))
                .isEqualTo(1L);
        assertThat(count("""
                select count(token) from UserFcmToken token
                where token.user.id = :userId
                """, withdrawingUser.getId())).isZero();
        assertThat(count("select count(workspace) from Workspace workspace", withdrawingUser.getId()))
                .isEqualTo(1L);
    }

    private User persistUser(String email, String friendCode) {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .nickname(friendCode)
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persist(user);
        return user;
    }

    private long count(String jpql, Long userId) {
        var query = entityManager.createQuery(jpql, Long.class);
        if (jpql.contains(":userId")) {
            query.setParameter("userId", userId);
        }
        return query.getSingleResult();
    }
}
