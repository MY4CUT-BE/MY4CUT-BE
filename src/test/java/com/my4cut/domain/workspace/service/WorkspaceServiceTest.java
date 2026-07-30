package com.my4cut.domain.workspace.service;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceDeleteResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceUpdateRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberService workspaceMemberService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("create workspace success")
    void createWorkspace_Success() {
        Long userId = 1L;
        User user = createUser(userId, "owner");
        WorkspaceCreateRequestDto requestDto = new WorkspaceCreateRequestDto("new workspace");
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                1L,
                "new workspace",
                userId,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now(),
                false,
                1,
                List.of(userId),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(workspaceRepository.save(any(Workspace.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(workspaceMemberService.convertToInfoDto(any(Workspace.class))).willReturn(responseDto);

        WorkspaceInfoResponseDto result = workspaceService.createWorkspace(requestDto, userId);

        assertThat(result.name()).isEqualTo("new workspace");
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
        verify(workspaceMemberService, times(1)).addMember(any(Workspace.class), any(User.class));
    }

    @Test
    @DisplayName("create default workspace success")
    void createDefaultWorkspace_Success() {
        User owner = createUser(1L, "owner");
        LocalDateTime beforeCreation = LocalDateTime.now();
        given(workspaceRepository.save(any(Workspace.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        workspaceService.createDefaultWorkspace(owner);

        LocalDateTime afterCreation = LocalDateTime.now();
        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(workspaceCaptor.capture());

        Workspace workspace = workspaceCaptor.getValue();
        assertThat(workspace.getName()).isEqualTo("포토리의 스페이스");
        assertThat(workspace.getOwner()).isSameAs(owner);
        assertThat(workspace.getExpiresAt())
                .isBetween(beforeCreation.plusDays(7), afterCreation.plusDays(7));
        verify(workspaceMemberService).addMember(workspace, owner);
    }

    @Test
    @DisplayName("update workspace success")
    void updateWorkspace_Success() {
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "old name", owner);
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("updated name");
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                workspaceId,
                "updated name",
                userId,
                workspace.getExpiresAt(),
                workspace.getCreatedAt(),
                false,
                1,
                List.of(userId),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null
        );

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberService.convertToInfoDto(workspace)).willReturn(responseDto);

        WorkspaceInfoResponseDto result = workspaceService.updateWorkspace(workspaceId, updateDto, userId);

        assertThat(result.name()).isEqualTo("updated name");
        assertThat(workspace.getName()).isEqualTo("updated name");
    }

    @Test
    @DisplayName("update workspace fails when user is not owner")
    void updateWorkspace_Fail_NotOwner() {
        Long workspaceId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L;
        User owner = createUser(ownerId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "workspace", owner);
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("try update");

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.updateWorkspace(workspaceId, updateDto, otherUserId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
    }

    @Test
    @DisplayName("update workspace fails when expired")
    void updateWorkspace_Fail_Expired() {
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "expired workspace", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("try update");

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.updateWorkspace(workspaceId, updateDto, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("get workspace info success")
    void getWorkspaceInfo_Success() {
        Long workspaceId = 1L;
        User owner = createUser(1L, "owner");
        Workspace workspace = createWorkspace(workspaceId, "workspace", owner);
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                workspaceId,
                "workspace",
                1L,
                workspace.getExpiresAt(),
                workspace.getCreatedAt(),
                false,
                2,
                List.of(1L, 3L),
                List.of(
                        "https://example.com/owner.png",
                        "https://example.com/member.png"
                ),
                List.of(2L),
                "COMMENT",                //  recentActivityType
                "member",                 //  recentActivityUserNickname
                LocalDateTime.now().minusMinutes(5)
        );

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberService.convertToInfoDto(workspace)).willReturn(responseDto);

        WorkspaceInfoResponseDto result = workspaceService.getWorkspaceInfo(workspaceId, 1L);

        assertThat(result.id()).isEqualTo(workspaceId);
        assertThat(result.name()).isEqualTo("workspace");
        assertThat(result.isFinal()).isFalse();
        assertThat(result.memberIds()).containsExactly(1L, 3L);
        assertThat(result.pendingInvitationUserIds()).containsExactly(2L);
        assertThat(result.alreadyInvitedFriendIds()).containsExactly(3L, 2L);
    }

    @Test
    @DisplayName("get workspace info fails for non member")
    void getWorkspaceInfo_Fail_NotMember() {
        Long workspaceId = 1L;
        Long otherUserId = 2L;
        User owner = createUser(1L, "owner");
        Workspace workspace = createWorkspace(workspaceId, "workspace", owner);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(workspaceMemberService.isWorkspaceMember(workspaceId, otherUserId)).willReturn(false);

        assertThatThrownBy(() -> workspaceService.getWorkspaceInfo(workspaceId, otherUserId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
    }

    @Test
    @DisplayName("get workspace info fails when expired")
    void getWorkspaceInfo_Fail_Expired() {
        Long workspaceId = 1L;
        User owner = createUser(1L, "owner");
        Workspace workspace = createWorkspace(workspaceId, "expired workspace", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.getWorkspaceInfo(workspaceId, 1L))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("delete workspace success")
    void deleteWorkspace_Success() {
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "workspace", owner);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        WorkspaceDeleteResponseDto result = workspaceService.deleteWorkspace(workspaceId, userId);

        assertThat(workspace.getDeletedAt()).isNotNull();
        assertThat(result.ownerId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("delete workspace fails when expired")
    void deleteWorkspace_Fail_Expired() {
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "expired workspace", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.deleteWorkspace(workspaceId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
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
