package com.my4cut.domain.workspace.service;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInvitationResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 워크스페이스 초대 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-02-08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberService workspaceMemberService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final NotificationService notificationService;

    /**
     * 멤버를 초대합니다. (초대장 생성 및 알림 발송)
     * @param dto 초대 요청 정보 DTO
     * @param inviterId 초대하는 유저 ID
     */
    @Transactional
    public void inviteMembers(WorkspaceInviteRequestDto dto, Long inviterId) {
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(dto.workspaceId())
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        if (!workspace.getOwner().getId().equals(inviterId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_OWNER);
        }

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));

        for (Long inviteeId : dto.userIds()) {
            User invitee = userRepository.findById(inviteeId)
                    .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));

            // 이미 멤버인지 확인
            if (workspaceMemberRepository.findByWorkspaceAndUser(workspace, invitee).isPresent()) {
                continue;
            }

            // 이미 보낸 대기 중인 초대가 있는지 확인 (중복 초대 방지)
            if (workspaceInvitationRepository.findByWorkspaceIdAndInviteeIdAndStatus(
                    workspace.getId(), inviteeId, InvitationStatus.PENDING).isPresent()) {
                continue;
            }

            WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                    .workspace(workspace)
                    .invitee(invitee)
                    .inviter(inviter)
                    .build();
            WorkspaceInvitation savedInvitation = workspaceInvitationRepository.save(invitation);

            // 초대 알림 발송
            notificationService.sendWorkspaceInviteNotification(invitee, inviter, workspace, savedInvitation.getId());
        }
    }

    /**
     * 내가 받은 대기 중인 초대 목록을 조회합니다.
     * @param userId 유저 ID
     * @return 초대 정보 DTO 리스트
     */
    public List<WorkspaceInvitationResponseDto> getMyInvitations(Long userId) {
        return workspaceInvitationRepository.findAllByInviteeIdAndStatus(userId, InvitationStatus.PENDING)
                .stream()
                .map(invitation -> new WorkspaceInvitationResponseDto(
                        invitation.getId(),
                        invitation.getWorkspace().getName(),
                        invitation.getInviter().getNickname(),
                        invitation.getStatus(),
                        invitation.getCreatedAt()))
                .toList();
    }

    /**
     * 초대를 수락합니다.
     */
    @Transactional
    public void acceptInvitation(Long invitationId, Long userId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        invitation.accept();
        workspaceMemberService.addMember(invitation.getWorkspace(), invitation.getInvitee());
    }

    /**
     * 초대를 거절합니다.
     */
    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        invitation.reject();
    }
}
