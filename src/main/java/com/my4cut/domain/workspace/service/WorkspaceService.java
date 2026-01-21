package com.my4cut.domain.workspace.service;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceUpdateRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

        private final WorkspaceRepository workspaceRepository;
        private final WorkspaceMemberService workspaceMemberService;
        private final UserRepository userRepository; // TODO: UserService가 완성되면 UserService를 통해 유저를 조회하도록 변경

        /**
         * 새로운 워크스페이스를 생성하고 생성자를 멤버로 등록합니다.
         */
        @Transactional
        public WorkspaceInfoResponseDto createWorkspace(WorkspaceCreateRequestDto dto, Long ownerId) {
                User owner = userRepository.findById(ownerId)
                                .orElseThrow(() -> new RuntimeException("User not found")); // 공통 유저 예외 적용 필요

                Workspace workspace = Workspace.builder()
                                .name(dto.name())
                                .owner(owner)
                                .expiresAt(LocalDateTime.now().plusMonths(1))
                                .build();

                workspaceRepository.save(workspace);

                // 생성자를 첫 번째 멤버로 추가
                workspaceMemberService.addMember(workspace, owner);

                return convertToInfoDto(workspace);
        }

        /**
         * 워크스페이스 정보를 수정합니다. (소유자만 가능)
         */
        @Transactional
        public WorkspaceInfoResponseDto updateWorkspace(Long workspaceId, WorkspaceUpdateRequestDto dto, Long userId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

                if (!workspace.getOwner().getId().equals(userId)) {
                        throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
                }

                workspace.setName(dto.name());
                return convertToInfoDto(workspace);
        }

        /**
         * 워크스페이스 단건 정보를 조회합니다.
         */
        public WorkspaceInfoResponseDto getWorkspaceInfo(Long workspaceId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
                return convertToInfoDto(workspace);
        }

        /**
         * 워크스페이스를 삭제(Soft Delete)합니다. (소유자만 가능)
         */
        @Transactional
        public void deleteWorkspace(Long workspaceId, Long userId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

                if (!workspace.getOwner().getId().equals(userId)) {
                        throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
                }

                workspace.setDeletedAt(LocalDateTime.now());
        }

        /**
         * 사용자가 참여 중인 워크스페이스 목록을 조회합니다.
         */
        public List<WorkspaceInfoResponseDto> getMyWorkspaces(Long userId) {
                return workspaceMemberService.getMyWorkspaces(userId);
        }

        private WorkspaceInfoResponseDto convertToInfoDto(Workspace workspace) {
                return new WorkspaceInfoResponseDto(
                                workspace.getId(),
                                workspace.getName(),
                                workspace.getOwner().getId(),
                                workspace.getExpiresAt(),
                                workspace.getCreatedAt());
        }
}
