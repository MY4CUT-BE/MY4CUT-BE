package com.my4cut.domain.workspace.service;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private WorkspaceMemberService workspaceMemberService;

    @Test
    @DisplayName("멤버 추가 성공: 워크스페이스에 유저를 멤버로 등록한다")
    void addMember_Success() {
        // Arrange
        Workspace workspace = Workspace.builder().name("워크스페이스").build();
        User user = User.builder().nickname("멤버").build();

        // Act
        workspaceMemberService.addMember(workspace, user);

        // Assert
        verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("워크스페이스 나가기 성공")
    void leaveWorkspace_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 2L;
        User owner = createUser(1L, "주인");
        User memberUser = createUser(userId, "멤버");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", owner);
        WorkspaceMember member = WorkspaceMember.builder().workspace(workspace).user(memberUser).build();

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
        given(workspaceMemberRepository.findByWorkspaceAndUser(workspace, memberUser)).willReturn(Optional.of(member));

        // Act
        workspaceMemberService.leaveWorkspace(workspaceId, userId);

        // Assert
        verify(workspaceMemberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("워크스페이스 나가기 실패: 소유자는 나갈 수 없다")
    void leaveWorkspace_Fail_OwnerCannotLeave() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", owner);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));
        given(userRepository.findById(userId)).willReturn(Optional.of(owner));

        // Act & Assert
        assertThatThrownBy(() -> workspaceMemberService.leaveWorkspace(workspaceId, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
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
