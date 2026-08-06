package com.my4cut.domain.workspace.service;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;
import com.my4cut.domain.notification.repository.NotificationRepository;
import com.my4cut.domain.notification.service.FcmService;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DataJpaTest
class WorkspaceDeletionIntegrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceInvitationRepository workspaceInvitationRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("스페이스 삭제 시 대기 초대와 알림이 삭제되고 이후 수락할 수 없다")
    void deleteWorkspace_RemovesPendingInvitationAndNotification() {
        User inviter = persistUser("inviter@test.com", "inviter", "INVITER1");
        User invitee = persistUser("invitee@test.com", "invitee", "INVITEE1");
        Workspace workspace = Workspace.builder()
                .name("스페이스123")
                .creator(inviter)
                .build();
        entityManager.persist(workspace);

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .inviter(inviter)
                .invitee(invitee)
                .build();
        entityManager.persist(invitation);
        entityManager.flush();

        Notification notification = Notification.builder()
                .user(invitee)
                .type(NotificationType.WORKSPACE_INVITE)
                .senderId(inviter.getId())
                .workspaceId(workspace.getId())
                .referenceId(invitation.getId())
                .isRead(false)
                .build();
        entityManager.persist(notification);
        entityManager.flush();
        entityManager.clear();

        WorkspaceMemberService memberService = mock(WorkspaceMemberService.class);
        given(memberService.isWorkspaceMember(workspace.getId(), inviter.getId())).willReturn(true);
        NotificationService notificationService = new NotificationService(
                mock(UserFcmTokenRepository.class),
                notificationRepository,
                userRepository,
                workspaceRepository,
                mock(FcmService.class)
        );
        WorkspaceService workspaceService = new WorkspaceService(
                workspaceRepository,
                memberService,
                userRepository,
                workspaceInvitationRepository,
                notificationService
        );

        workspaceService.deleteWorkspace(workspace.getId(), inviter.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(workspaceInvitationRepository.findById(invitation.getId())).isEmpty();
        assertThat(notificationRepository.findById(notification.getId())).isEmpty();
        assertThat(notificationRepository
                .findVisibleByUserIdOrderByCreatedAtDesc(invitee.getId(), PageRequest.of(0, 8)))
                .isEmpty();
        assertThat(notificationRepository.countVisibleUnreadByUserId(invitee.getId())).isZero();

        WorkspaceInvitationService invitationService = new WorkspaceInvitationService(
                workspaceInvitationRepository,
                workspaceRepository,
                userRepository,
                memberService,
                workspaceMemberRepository,
                notificationService
        );
        assertThatThrownBy(() -> invitationService.acceptInvitation(invitation.getId(), invitee.getId()))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.INVITATION_NOT_FOUND);
    }

    private User persistUser(String email, String nickname, String friendCode) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .loginType(LoginType.EMAIL)
                .friendCode(friendCode)
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persist(user);
        return user;
    }
}
