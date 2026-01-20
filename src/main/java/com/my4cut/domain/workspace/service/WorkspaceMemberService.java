package com.my4cut.domain.workspace.service;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 멤버 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    /**
     * 워크스페이스에 새로운 멤버를 수동으로 추가합니다. (워크스페이스 생성 시 등 내부용)
     */
    @Transactional
    public void addMember(Workspace workspace, User user) {
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
        workspaceMemberRepository.save(member);
    }

    /**
     * 사용자가 참여 중인 워크스페이스 목록을 조회합니다.
     */
    public List<WorkspaceInfoResponseDto> getMyWorkspaces(Long userId) {
        return workspaceMemberRepository.findAllByUserId(userId).stream()
                .map(member -> convertToInfoDto(member.getWorkspace()))
                .toList();
    }

    /**
     * 사용자가 워크스페이스에서 나갑니다.
     */
    @Transactional
    public void leaveWorkspace(Long workspaceId, Long userId) {
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (workspace.getOwner().getId().equals(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
        }

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        workspaceMemberRepository.delete(member);
    }


    private WorkspaceInfoResponseDto convertToInfoDto(Workspace workspace) {
        return new WorkspaceInfoResponseDto(
                workspace.getId(),
                workspace.getName(),
                     workspace.getExpiresAt(),
                workspace.getCreatedAt());
    }
}
