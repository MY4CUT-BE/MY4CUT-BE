package com.my4cut.domain.workspace.service;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInvitationResponseDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 워크스페이스 초대 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-02-08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceInvitationService {

    private static final int MAX_WORKSPACE_MEMBERS = 10;
    private static final int MAX_INVITE_USERS_PER_REQUEST = MAX_WORKSPACE_MEMBERS - 1;

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

        // 한 번에 최대 9명까지만 초대 가능
        if (dto == null || dto.userIds() == null || dto.userIds().size() > MAX_INVITE_USERS_PER_REQUEST) {
            throw new WorkspaceException(WorkspaceErrorCode.MAX_INVITE_USERS_EXCEEDED);
        }

        if (dto.workspaceId() == null) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        }

        // 같은 스페이스로 들어오는 초대 요청을 직렬화해 남은 자리 계산과 저장을 원자적으로 처리한다.
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(dto.workspaceId())
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        if (workspace.isExpired()) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_EXPIRED);
        }

        if (!workspaceMemberService.isWorkspaceMember(workspace.getId(), inviterId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));
        if (inviter.isDeleted()) {
            throw new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND);
        }

        Set<Long> occupiedUserIds = new HashSet<>();
        for (WorkspaceMember member : workspaceMemberRepository.findAllByWorkspaceId(workspace.getId())) {
            occupiedUserIds.add(member.getUser().getId());
        }
        workspaceInvitationRepository.findAllByWorkspaceIdAndStatus(workspace.getId(), InvitationStatus.PENDING)
                .forEach(invitation -> occupiedUserIds.add(invitation.getInvitee().getId()));

        List<User> newInvitees = new ArrayList<>();
        for (Long inviteeId : new LinkedHashSet<>(dto.userIds())) {
            if (inviteeId == null) {
                throw new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND);
            }

            if (occupiedUserIds.contains(inviteeId)) {
                continue;
            }

            User invitee = userRepository.findById(inviteeId)
                    .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));
            if (invitee.isDeleted()) {
                throw new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND);
            }
            newInvitees.add(invitee);
        }

        // 초대장/알림을 만들기 전에 요청 전체의 정원을 검사한다.
        if (occupiedUserIds.size() + newInvitees.size() > MAX_WORKSPACE_MEMBERS) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_CAPACITY_EXCEEDED);
        }

        for (User invitee : newInvitees) {
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
        return workspaceInvitationRepository.findAllByInviteeIdAndStatusIn(
                        userId,
                        List.of(
                                InvitationStatus.PENDING,
                                InvitationStatus.REJECTED
                        )
                )
                .stream()
                .map(invitation -> new WorkspaceInvitationResponseDto(
                        invitation.getId(),
                        invitation.getWorkspace().getName(),
                        invitation.getInviter().getNickname(),
                        invitation.getInviter().getProfileImageUrl(),
                        invitation.getStatus(),
                        invitation.getCreatedAt()))
                .toList();
    }

    /**
     * 초대를 수락합니다.
     */
    // 초대 조회 후 스페이스 잠금을 기다리는 동안 다른 수락이 커밋될 수 있으므로,
    // 잠금 획득 이후의 상태와 멤버 수를 최신 커밋 기준으로 다시 읽는다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void acceptInvitation(Long invitationId, Long userId) {
        Long workspaceId = workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNullForUpdate(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (workspace.isExpired()) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_EXPIRED);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        if (invitation.getInvitee().isDeleted()) {
            throw new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND);
        }

        // 과거에 중복 생성된 초대가 남아 있어도 멤버를 중복 등록하지 않는다.
        if (workspaceMemberService.isWorkspaceMember(workspace.getId(), userId)) {
            invitation.accept();
            notificationService.deleteWorkspaceInviteNotification(invitation.getInvitee(), invitation.getId());
            return;
        }

        // 스페이스 정원 확인
        if (workspaceMemberRepository.countByWorkspace(workspace) >= MAX_WORKSPACE_MEMBERS) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_FULL);
        }

        invitation.accept();
        workspaceMemberService.addMember(invitation.getWorkspace(), invitation.getInvitee());

        // 초대 수락 후에는 해당 초대 알림을 제거해 처리 완료 알림이 알림창에 남지 않게 한다.
        notificationService.deleteWorkspaceInviteNotification(invitation.getInvitee(), invitation.getId());

    }

    /**
     * 초대를 거절합니다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectInvitation(Long invitationId, Long userId) {
        Long workspaceId = workspaceInvitationRepository.findWorkspaceIdByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));
        workspaceRepository.findByIdForUpdate(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByIdAndInviteeId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        invitation.reject();

        // 초대 거절 후에는 더 이상 응답할 수 없는 초대 알림을 즉시 제거한다.
        notificationService.deleteWorkspaceInviteNotification(invitation.getInvitee(), invitation.getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelInvitation(Long invitationId, Long userId) {
        Long workspaceId = workspaceInvitationRepository.findWorkspaceIdByIdAndInviterId(invitationId, userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));
        workspaceRepository.findByIdForUpdate(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceInvitation invitation =
                workspaceInvitationRepository.findByIdAndInviterId(invitationId, userId)
                        .orElseThrow(() ->
                                new WorkspaceException(WorkspaceErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new WorkspaceException(WorkspaceErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        // 초대 알림 삭제
        notificationService.deleteWorkspaceInviteNotification(
                invitation.getInvitee(),
                invitation.getId()
        );

        // 초대 삭제
        workspaceInvitationRepository.delete(invitation);
    }

}
