package com.my4cut.domain.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberService workspaceMemberService;

    @Mock
    private WorkspaceInvitationService workspaceInvitationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("워크스페이스 상세 조회 성공: 초대 대기 유저와 현재 멤버 유저 ID를 함께 반환한다")
    void getWorkspaceInfo_success_returnsInvitationAndMemberIds() {
        // Arrange
        Long workspaceId = 10L;
        Long ownerId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(3);

        User owner = createUser(ownerId, "owner");
        Workspace workspace = createWorkspace(workspaceId, "우리 가족 앨범", owner, createdAt, expiresAt);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId))
                .willReturn(Optional.of(workspace));
        given(workspaceMemberService.getMemberCount(workspaceId)).willReturn(2);
        given(workspaceMemberService.getMemberProfiles(workspaceId))
                .willReturn(List.of("https://example.com/p1.png", "https://example.com/p2.png"));
        given(workspaceInvitationService.getPendingInvitationUserIds(workspaceId))
                .willReturn(List.of(2L, 3L));
        given(workspaceMemberService.getMemberIds(workspaceId))
                .willReturn(List.of(1L, 4L));

        // Act
        WorkspaceDto.InfoResponse result = workspaceService.getWorkspaceInfo(workspaceId);

        // Assert
        assertThat(result.id()).isEqualTo(workspaceId);
        assertThat(result.name()).isEqualTo("우리 가족 앨범");
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.memberCount()).isEqualTo(2);
        assertThat(result.memberProfiles())
                .containsExactly("https://example.com/p1.png", "https://example.com/p2.png");
        assertThat(result.pendingInvitationUserIds()).containsExactly(2L, 3L);
        assertThat(result.memberIds()).containsExactly(1L, 4L);
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder()
                .email(nickname + "@test.com")
                .password("password")
                .nickname(nickname)
                .loginType(LoginType.EMAIL)
                .friendCode("FRIEND-" + id)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Workspace createWorkspace(
            Long id,
            String name,
            User owner,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        Workspace workspace = Workspace.builder()
                .name(name)
                .owner(owner)
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(workspace, "id", id);
        ReflectionTestUtils.setField(workspace, "createdAt", createdAt);
        return workspace;
    }
}
