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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;

/**
 * 워크스페이스 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-02-08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

        private static final String DEFAULT_WORKSPACE_NAME = "포토리의스페이스";
        private static final long WORKSPACE_EXPIRATION_DAYS = 7L;

        private final WorkspaceRepository workspaceRepository;
        private final WorkspaceMemberService workspaceMemberService;
        private final UserRepository userRepository; // TODO: UserService가 완성되면 UserService를 통해 유저를 조회하도록 변경
        private final WorkspaceInvitationRepository workspaceInvitationRepository;
        private final NotificationService notificationService;

        /**
         * 새로운 워크스페이스를 생성하고 생성자를 멤버로 등록합니다.
         * @param dto 워크스페이스 생성 정보 DTO
         * @param creatorId 생성자 ID
         * @return 생성된 워크스페이스 정보 DTO
         */
        @Transactional
        public WorkspaceInfoResponseDto createWorkspace(WorkspaceCreateRequestDto dto, Long creatorId) {
                User creator = userRepository.findById(creatorId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND)); // 공통 유저 예외 적용 필요

                Workspace workspace = createWorkspaceWithCreator(dto.name(), creator);

                return workspaceMemberService.convertToInfoDto(workspace);
        }

        /**
         * 신규 가입자를 위한 기본 튜토리얼 워크스페이스를 생성합니다.
         *
         * @param creator 신규 가입 사용자
         */
        @Transactional
        public void createDefaultWorkspace(User creator) {
                createWorkspaceWithCreator(DEFAULT_WORKSPACE_NAME, creator);
        }

        /**
         * 워크스페이스 정보를 수정합니다. (멤버만 가능)
         * @param workspaceId 워크스페이스 ID
         * @param dto 수정할 워크스페이스 정보 DTO
         * @param userId 유저 ID
         * @return 수정된 워크스페이스 정보 DTO
         */
        @Transactional
        public WorkspaceInfoResponseDto updateWorkspace(Long workspaceId, WorkspaceUpdateRequestDto dto, Long userId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

                checkWorkspaceExpiration(workspace);

                validateMembership(workspaceId, userId);

                workspace.setName(dto.name());
                return workspaceMemberService.convertToInfoDto(workspace);
        }

        /**
         * 워크스페이스 단건 정보를 조회합니다.
         * @param workspaceId 워크스페이스 ID
         * @return 워크스페이스 정보 DTO
         */
        public WorkspaceInfoResponseDto getWorkspaceInfo(Long workspaceId, Long userId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

                checkWorkspaceExpiration(workspace);

                validateMembership(workspaceId, userId);

                return workspaceMemberService.convertToInfoDto(workspace);
        }

        /**
         * 워크스페이스를 삭제(Soft Delete)합니다. (멤버만 가능)
         * @param workspaceId 워크스페이스 ID
         * @param userId 유저 ID
         */
        @Transactional
        public WorkspaceDeleteResponseDto deleteWorkspace(Long workspaceId, Long userId) {
                Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

                checkWorkspaceExpiration(workspace);

                validateMembership(workspaceId, userId);

                workspace.setDeletedAt(LocalDateTime.now());
                workspaceInvitationRepository.deleteAllByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING);
                notificationService.deleteWorkspaceInviteNotifications(workspaceId);
                return new WorkspaceDeleteResponseDto();
        }

        /**
         * 사용자가 참여 중인 워크스페이스 목록을 조회합니다. (삭제된 워크스페이스 제외)
         * @param userId 유저 ID
         * @return 참여 중인 워크스페이스 정보 DTO 리스트
         */
        public List<WorkspaceInfoResponseDto> getMyWorkspaces(Long userId) {
                return workspaceMemberService.getMyWorkspaces(userId);
        }

        private void checkWorkspaceExpiration(Workspace workspace) {
                if (workspace.isExpired()) {
                        throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_EXPIRED);
                }
        }

        private Workspace createWorkspaceWithCreator(String name, User creator) {
                Workspace workspace = Workspace.builder()
                                .name(name)
                                .creator(creator)
                                .expiresAt(LocalDateTime.now().plusDays(WORKSPACE_EXPIRATION_DAYS))
                                .build();

                Workspace savedWorkspace = workspaceRepository.save(workspace);
                workspaceMemberService.addMember(savedWorkspace, creator);
                return savedWorkspace;
        }

        private void validateMembership(Long workspaceId, Long userId) {
                if (!workspaceMemberService.isWorkspaceMember(workspaceId, userId)) {
                        throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
                }
        }

}
