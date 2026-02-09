package com.my4cut.domain.workspace.service;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceUpdateRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
    @DisplayName("워크스페이스 생성 성공: 생성자를 멤버로 자동 등록한다")
    void createWorkspace_Success() {
        // Arrange
        Long userId = 1L;
        User user = createUser(userId, "주인");
        WorkspaceCreateRequestDto requestDto = new WorkspaceCreateRequestDto("새 워크스페이스");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // Act
        WorkspaceInfoResponseDto result = workspaceService.createWorkspace(requestDto, userId);

        // Assert
        assertThat(result.name()).isEqualTo("새 워크스페이스");
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
        verify(workspaceMemberService, times(1)).addMember(any(Workspace.class), any(User.class));
    }

    @Test
    @DisplayName("워크스페이스 수정 성공: 소유자가 이름을 수정한다")
    void updateWorkspace_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "기존 이름", owner);
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("수정된 이름");

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act
        WorkspaceInfoResponseDto result = workspaceService.updateWorkspace(workspaceId, updateDto, userId);

        // Assert
        assertThat(result.name()).isEqualTo("수정된 이름");
        assertThat(workspace.getName()).isEqualTo("수정된 이름");
    }

    @Test
    @DisplayName("워크스페이스 수정 실패: 소유자가 아닌 경우 예외가 발생한다")
    void updateWorkspace_Fail_NotOwner() {
        // Arrange
        Long workspaceId = 1L;
        Long ownerId = 1L;
        Long otherUserId = 2L;
        User owner = createUser(ownerId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "워크스페이스", owner);
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("수정 시도");

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspaceService.updateWorkspace(workspaceId, updateDto, otherUserId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
    }

    @Test
    @DisplayName("워크스페이스 수정 실패: 만료된 워크스페이스인 경우 예외 발생")
    void updateWorkspace_Fail_Expired() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "만료된 워크스페이스", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));
        WorkspaceUpdateRequestDto updateDto = new WorkspaceUpdateRequestDto("수정 시도");

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspaceService.updateWorkspace(workspaceId, updateDto, userId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 성공")
    void getWorkspaceInfo_Success() {
        // Arrange
        Long workspaceId = 1L;
        User owner = createUser(1L, "주인");
        Workspace workspace = createWorkspace(workspaceId, "조회용", owner);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act
        WorkspaceInfoResponseDto result = workspaceService.getWorkspaceInfo(workspaceId);

        // Assert
        assertThat(result.id()).isEqualTo(workspaceId);
        assertThat(result.name()).isEqualTo("조회용");
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 실패: 만료된 워크스페이스인 경우 예외 발생")
    void getWorkspaceInfo_Fail_Expired() {
        // Arrange
        Long workspaceId = 1L;
        User owner = createUser(1L, "주인");
        Workspace workspace = createWorkspace(workspaceId, "만료된 워크스페이스", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
        assertThatThrownBy(() -> workspaceService.getWorkspaceInfo(workspaceId))
                .isInstanceOf(WorkspaceException.class)
                .hasFieldOrPropertyWithValue("errorCode", WorkspaceErrorCode.WORKSPACE_EXPIRED);
    }

    @Test
    @DisplayName("워크스페이스 삭제 성공: 소유자가 삭제하면 soft delete 된다")
    void deleteWorkspace_Success() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "삭제될 워크스페이스", owner);

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act
        workspaceService.deleteWorkspace(workspaceId, userId);

        // Assert
        assertThat(workspace.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("워크스페이스 삭제 실패: 만료된 워크스페이스인 경우 예외 발생")
    void deleteWorkspace_Fail_Expired() {
        // Arrange
        Long workspaceId = 1L;
        Long userId = 1L;
        User owner = createUser(userId, "주인");
        Workspace workspace = createWorkspace(workspaceId, "만료된 워크스페이스", owner);
        workspace.setExpiresAt(LocalDateTime.now().minusDays(1));

        given(workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)).willReturn(Optional.of(workspace));

        // Act & Assert
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
