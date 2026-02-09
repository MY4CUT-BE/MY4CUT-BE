package com.my4cut.domain.workspace.service;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock private WorkspaceInvitationRepository workspaceInvitationRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceMemberService workspaceMemberService;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private WorkspaceInvitationService workspaceInvitationService;

    @Test
    @DisplayName("멤버 초대 성공: 소유자가 멤버를 초대하면 초대장이 생성되고 알림이 발송된다")
    void inviteMembers_Success() {
        // Arrange
        Long inviterId = 1L;
        Long inviteeId = 2L;
        Long workspaceId = 10L;
        User inviter = createUser(inviterId, "초대자");
        User invitee = createUser(inviteeId, "피초대자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", inviter);
        WorkspaceInviteRequestDto requestDto = new WorkspaceInviteRequestDto(workspaceId, List.of(inviteeId));

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(userRepository.findById(inviterId)).willReturn(Optional.of(inviter));
        given(userRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, invitee)).willReturn(Optional.empty());
        given(workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(workspaceId, inviteeId, InvitationStatus.PENDING))
                .willReturn(Optional.empty());
        
        WorkspaceInvitation invitation = WorkspaceInvitation.builder().workspace(workspace).invitee(invitee).inviter(inviter).build();
        ReflectionTestUtils.setField(invitation, "id", 100L);
        given(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).willReturn(invitation);

        // Act
        workspaceInvitationService.inviteMembers(requestDto, inviterId);

        // Assert
        verify(workspaceInvitationRepository, times(1)).save(any(WorkspaceInvitation.class));
        verify(notificationService, times(1)).sendWorkspaceInviteNotification(eq(invitee), eq(inviter), eq(workspace), anyLong());
    }

    @Test
    @DisplayName("초대 수락 성공: 상태가 ACCEPTED로 변경되고 멤버로 추가된다")
    void acceptInvitation_Success() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        User invitee = createUser(userId, "피초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", createUser(1L, "주인"));
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitee(invitee)
                .inviter(createUser(1L, "주인"))
                .build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.PENDING);

        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)).willReturn(Optional.of(invitation));

        // Act
        workspaceInvitationService.acceptInvitation(invitationId, userId);

        // Assert
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(workspaceMemberService, times(1)).addMember(workspace, invitee);
    }

    @Test
    @DisplayName("초대 수락 실패: 이미 처리된 초대인 경우 예외 발생")
    void acceptInvitation_Fail_AlreadyProcessed() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        WorkspaceInvitation invitation = WorkspaceInvitation.builder().build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.ACCEPTED);

        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)).willReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> workspaceInvitationService.acceptInvitation(invitationId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Workspace createWorkspace(Long id, String name, User owner) {
        Workspace workspace = Workspace.builder().name(name).owner(owner).build();
        ReflectionTestUtils.setField(workspace, "id", id);
        return workspace;
    }
}
