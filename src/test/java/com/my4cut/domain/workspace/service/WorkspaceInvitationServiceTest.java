package com.my4cut.domain.workspace.service;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    @DisplayName("멤버 초대 성공: 워크스페이스 멤버가 초대하면 초대장이 생성되고 알림이 발송된다")
    void inviteMembers_Success() {
        // Arrange
        Long inviterId = 1L;
        Long inviteeId = 2L;
        Long workspaceId = 10L;
        User inviter = createUser(inviterId, "초대자");
        User invitee = createUser(inviteeId, "피초대자");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", inviter);
        WorkspaceInviteRequestDto requestDto = new WorkspaceInviteRequestDto(workspaceId, List.of(inviteeId));

        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberService.isWorkspaceMember(workspaceId, inviterId)).willReturn(true);
        given(userRepository.findById(inviterId)).willReturn(Optional.of(inviter));
        given(userRepository.findById(inviteeId)).willReturn(Optional.of(invitee));
        given(workspaceMemberRepository.findAllByWorkspaceId(workspaceId)).willReturn(List.of(member(workspace, inviter)));
        given(workspaceInvitationRepository.findAllByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING))
                .willReturn(List.of());
        
        WorkspaceInvitation invitation = WorkspaceInvitation.builder().workspace(workspace).invitee(invitee).inviter(inviter).build();
        ReflectionTestUtils.setField(invitation, "id", 100L);
        given(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).willReturn(invitation);

        // Act
        workspaceInvitationService.inviteMembers(requestDto, inviterId);

        // Assert
        verify(workspaceInvitationRepository, times(1)).save(any(WorkspaceInvitation.class));
        verify(notificationService, times(1)).sendWorkspaceInviteNotification(eq(invitee), eq(inviter), eq(workspace), anyLong());
        verify(workspaceRepository).findByIdAndDeletedAtIsNullForUpdate(workspaceId);
    }

    @Test
    @DisplayName("멤버 초대 실패: 만료된 워크스페이스인 경우 예외 발생")
    void inviteMembers_Fail_Expired() {
        // Arrange
        Long inviterId = 1L;
        User inviter = createUser(inviterId, "초대자");
        WorkspaceInviteRequestDto requestDto = new WorkspaceInviteRequestDto(10L, List.of(2L));
        Workspace workspace = createWorkspace(10L, "만료됨", inviter);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(10L)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspaceInvitationService.inviteMembers(requestDto, inviterId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("멤버 2명과 대기 초대 7명이 있으면 마지막 한 자리만 초대할 수 있다")
    void inviteMembers_Success_OneRemainingSeat() {
        User inviter = createUser(1L, "초대자");
        User otherMember = createUser(2L, "기존 멤버");
        User invitee = createUser(20L, "새 멤버");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        List<WorkspaceInvitation> pending = pendingInvitations(workspace, inviter, 7);
        stubInviteContext(workspace, inviter, List.of(inviter, otherMember), pending);
        given(userRepository.findById(invitee.getId())).willReturn(Optional.of(invitee));
        WorkspaceInvitation saved = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        ReflectionTestUtils.setField(saved, "id", 100L);
        given(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).willReturn(saved);

        workspaceInvitationService.inviteMembers(
                new WorkspaceInviteRequestDto(workspace.getId(), List.of(invitee.getId())), inviter.getId());

        verify(workspaceInvitationRepository).save(any(WorkspaceInvitation.class));
        verify(notificationService).sendWorkspaceInviteNotification(invitee, inviter, workspace, 100L);
    }

    @Test
    @DisplayName("다른 멤버가 이미 초대한 인원도 합산하며 초과 요청은 전체 실패한다")
    void inviteMembers_Fail_SharedCapacityWithoutPartialCreation() {
        User inviter = createUser(1L, "초대자");
        User otherMember = createUser(2L, "다른 초대자");
        User first = createUser(20L, "첫 번째");
        User second = createUser(21L, "두 번째");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        stubInviteContext(workspace, inviter, List.of(inviter, otherMember),
                pendingInvitations(workspace, otherMember, 7));
        given(userRepository.findById(first.getId())).willReturn(Optional.of(first));
        given(userRepository.findById(second.getId())).willReturn(Optional.of(second));

        assertThatThrownBy(() -> workspaceInvitationService.inviteMembers(
                new WorkspaceInviteRequestDto(workspace.getId(), List.of(first.getId(), second.getId())),
                inviter.getId()))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.INVITATION_CAPACITY_EXCEEDED);

        verify(workspaceInvitationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("이미 참여하거나 대기 중인 사용자와 요청 내 중복 ID는 새 자리로 세지 않는다")
    void inviteMembers_Success_DeduplicatesOccupiedUsers() {
        User inviter = createUser(1L, "초대자");
        User otherMember = createUser(2L, "기존 멤버");
        User invitee = createUser(20L, "새 멤버");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        List<WorkspaceInvitation> pending = pendingInvitations(workspace, inviter, 7);
        stubInviteContext(workspace, inviter, List.of(inviter, otherMember), pending);
        given(userRepository.findById(invitee.getId())).willReturn(Optional.of(invitee));
        WorkspaceInvitation saved = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        ReflectionTestUtils.setField(saved, "id", 100L);
        given(workspaceInvitationRepository.save(any(WorkspaceInvitation.class))).willReturn(saved);

        workspaceInvitationService.inviteMembers(new WorkspaceInviteRequestDto(workspace.getId(),
                List.of(otherMember.getId(), pending.get(0).getInvitee().getId(),
                        invitee.getId(), invitee.getId())), inviter.getId());

        verify(workspaceInvitationRepository, times(1)).save(any(WorkspaceInvitation.class));
        verify(notificationService, times(1))
                .sendWorkspaceInviteNotification(invitee, inviter, workspace, 100L);
        verify(userRepository, never()).findById(otherMember.getId());
    }

    @Test
    @DisplayName("초대 대상 ID가 null이면 초대장이나 알림을 생성하지 않는다")
    void inviteMembers_Fail_NullInviteeId() {
        User inviter = createUser(1L, "초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        stubInviteContext(workspace, inviter, List.of(inviter), List.of());
        given(userRepository.findById(2L)).willReturn(Optional.of(createUser(2L, "유효한 대상")));

        assertThatThrownBy(() -> workspaceInvitationService.inviteMembers(
                new WorkspaceInviteRequestDto(workspace.getId(), Arrays.asList(2L, null)), inviter.getId()))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.USER_NOT_FOUND);

        verify(workspaceInvitationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("초대 수락 성공: 상태가 ACCEPTED로 변경되고 멤버로 추가된다")
    void acceptInvitation_Success() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        User inviter = createUser(1L, "주인");
        User invitee = createUser(userId, "피초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitee(invitee)
                .inviter(inviter)
                .build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.PENDING);

        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)).willReturn(Optional.of(invitation));

        // Act
        workspaceInvitationService.acceptInvitation(invitationId, userId);

        // Assert
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(workspaceMemberService, times(1)).addMember(workspace, invitee);
        verify(notificationService, times(1))
                .deleteWorkspaceInviteNotification(invitee, invitation.getId());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    @DisplayName("초대 수락 실패: 워크스페이스가 만료된 경우 예외 발생")
    void acceptInvitation_Fail_Expired() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        User invitee = createUser(userId, "피초대자");
        Workspace workspace = createWorkspace(10L, "만료됨", createUser(1L, "주인"));
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitee(invitee)
                .inviter(createUser(1L, "주인"))
                .build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.PENDING);

        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)).willReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> workspaceInvitationService.acceptInvitation(invitationId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("초대 수락 실패: 워크스페이스가 삭제된 경우 예외 발생")
    void acceptInvitation_Fail_Deleted() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        User invitee = createUser(userId, "피초대자");
        Workspace workspace = createWorkspace(10L, "삭제됨", createUser(1L, "주인"));
        workspace.setDeletedAt(LocalDateTime.now());

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitee(invitee)
                .inviter(createUser(1L, "주인"))
                .build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.PENDING);

        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> workspaceInvitationService.acceptInvitation(invitationId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(workspaceMemberService, never()).addMember(any(), any());
        verify(notificationService, never()).deleteWorkspaceInviteNotification(any(), anyLong());
    }

    @Test
    @DisplayName("초대 수락 실패: 이미 처리된 초대인 경우 예외 발생")
    void acceptInvitation_Fail_AlreadyProcessed() {
        // Arrange
        Long invitationId = 1L;
        Long userId = 2L;
        Workspace workspace = createWorkspace(10L, "워크스페이스", createUser(1L, "주인"));
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.ACCEPTED);

        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)).willReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() -> workspaceInvitationService.acceptInvitation(invitationId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("기존에 초과 발송된 초대라도 멤버 정원이 가득 차면 수락할 수 없다")
    void acceptInvitation_Fail_WorkspaceFull() {
        Long invitationId = 1L;
        Long inviteeId = 2L;
        User inviter = createUser(1L, "초대자");
        User invitee = createUser(inviteeId, "피초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, inviteeId))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(invitationId, inviteeId))
                .willReturn(Optional.of(invitation));
        given(workspaceMemberRepository.countByWorkspace(workspace)).willReturn(10L);

        assertThatThrownBy(() -> workspaceInvitationService.acceptInvitation(invitationId, inviteeId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_FULL);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(workspaceMemberService, never()).addMember(any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("이미 멤버인 사용자의 오래된 대기 초대는 중복 멤버 등록 없이 처리한다")
    void acceptInvitation_Success_AlreadyMemberWithoutDuplicate() {
        User inviter = createUser(1L, "초대자");
        User invitee = createUser(2L, "기존 멤버");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        ReflectionTestUtils.setField(invitation, "id", 100L);
        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(100L, invitee.getId()))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(100L, invitee.getId()))
                .willReturn(Optional.of(invitation));
        given(workspaceMemberService.isWorkspaceMember(workspace.getId(), invitee.getId()))
                .willReturn(true);

        workspaceInvitationService.acceptInvitation(100L, invitee.getId());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(workspaceMemberService, never()).addMember(any(), any());
        verify(notificationService).deleteWorkspaceInviteNotification(invitee, 100L);
    }

    @Test
    @DisplayName("초대 거절은 워크스페이스 잠금 후 최신 대기 상태를 확인한다")
    void rejectInvitation_Success_WithWorkspaceLock() {
        User inviter = createUser(1L, "초대자");
        User invitee = createUser(2L, "피초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        ReflectionTestUtils.setField(invitation, "id", 100L);
        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(100L, invitee.getId()))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviteeId(100L, invitee.getId()))
                .willReturn(Optional.of(invitation));

        workspaceInvitationService.rejectInvitation(100L, invitee.getId());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        verify(workspaceRepository).findByIdForUpdate(workspace.getId());
        verify(notificationService).deleteWorkspaceInviteNotification(invitee, 100L);
    }

    @Test
    @DisplayName("초대 취소는 워크스페이스 잠금 후 초대와 알림을 삭제한다")
    void cancelInvitation_Success_WithWorkspaceLock() {
        User inviter = createUser(1L, "초대자");
        User invitee = createUser(2L, "피초대자");
        Workspace workspace = createWorkspace(10L, "워크스페이스", inviter);
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace).inviter(inviter).invitee(invitee).build();
        ReflectionTestUtils.setField(invitation, "id", 100L);
        given(workspaceInvitationRepository.findWorkspaceIdByIdAndInviterId(100L, inviter.getId()))
                .willReturn(Optional.of(workspace.getId()));
        given(workspaceRepository.findByIdForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceInvitationRepository.findByIdAndInviterId(100L, inviter.getId()))
                .willReturn(Optional.of(invitation));

        workspaceInvitationService.cancelInvitation(100L, inviter.getId());

        verify(workspaceRepository).findByIdForUpdate(workspace.getId());
        verify(notificationService).deleteWorkspaceInviteNotification(invitee, 100L);
        verify(workspaceInvitationRepository).delete(invitation);
    }

    private void stubInviteContext(Workspace workspace, User inviter, List<User> members,
                                   List<WorkspaceInvitation> pending) {
        given(workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspace.getId()))
                .willReturn(Optional.of(workspace));
        given(workspaceMemberService.isWorkspaceMember(workspace.getId(), inviter.getId())).willReturn(true);
        given(userRepository.findById(inviter.getId())).willReturn(Optional.of(inviter));
        given(workspaceMemberRepository.findAllByWorkspaceId(workspace.getId()))
                .willReturn(members.stream().map(user -> member(workspace, user)).toList());
        given(workspaceInvitationRepository.findAllByWorkspaceIdAndStatus(workspace.getId(), InvitationStatus.PENDING))
                .willReturn(pending);
    }

    private List<WorkspaceInvitation> pendingInvitations(Workspace workspace, User inviter, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> WorkspaceInvitation.builder()
                        .workspace(workspace)
                        .inviter(inviter)
                        .invitee(createUser(100L + index, "대기 중"))
                        .build())
                .toList();
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder().nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private WorkspaceMember member(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    private Workspace createWorkspace(Long id, String name, User ignoredCreator) {
        Workspace workspace = Workspace.builder().name(name).build();
        ReflectionTestUtils.setField(workspace, "id", id);
        return workspace;
    }
}
