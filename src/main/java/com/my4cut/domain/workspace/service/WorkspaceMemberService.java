package com.my4cut.domain.workspace.service;

import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.image.service.ProfileImageUrlService;
import com.my4cut.domain.media.entity.MediaComment;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.repository.MediaCommentRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import com.my4cut.domain.workspace.dto.WorkspaceInvitationUserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

/**
 * 워크스페이스 멤버 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final ProfileImageUrlService profileImageUrlService;
    private final MediaCommentRepository mediaCommentRepository;
    private final FriendRepository friendRepository;

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
     * 사용자가 참여 중인 워크스페이스 목록을 조회합니다. (만료되거나 삭제된 워크스페이스 제외)
     * @param userId 유저 ID
     * @return 참여 중인 워크스페이스 정보 DTO 리스트
     */
    public List<WorkspaceInfoResponseDto> getMyWorkspaces(Long userId) {
        return workspaceMemberRepository.findAllByUserIdAndWorkspaceExpiresAtAfterAndWorkspaceDeletedAtIsNull(userId, LocalDateTime.now()).stream()
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
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        workspaceMemberRepository.delete(member);
    }


    public List<String> getMemberProfiles(Long workspaceId) {
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(member -> profileImageUrlService.toResponseUrl(
                        member.getUser().getProfileImageUrl()))
                .toList();
    }

    public List<Long> getMemberIds(Long workspaceId) {
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(member -> member.getUser().getId())
                .toList();
    }

    public int getMemberCount(Long workspaceId) {
        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId).size();
    }

    public boolean isWorkspaceMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public WorkspaceInfoResponseDto convertToInfoDto(Workspace workspace) {
        List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspace.getId());
        List<String> memberProfiles = members.stream()
                .map(member -> profileImageUrlService.toResponseUrl(
                        member.getUser().getProfileImageUrl()))
                .toList();
        List<Long> pendingInvitationUserIds = workspaceInvitationRepository
                .findAllByWorkspaceIdAndStatus(workspace.getId(), InvitationStatus.PENDING)
                .stream()
                .map(invitation -> invitation.getInvitee().getId())
                .toList();

        Optional<MediaFile> latestPhoto =
                mediaFileRepository.findTopByWorkspaceIdOrderByCreatedAtDesc(workspace.getId());

        Optional<MediaComment> latestComment =
                mediaCommentRepository.findTopByMediaFileWorkspaceIdOrderByCreatedAtDesc(workspace.getId());

        String recentActivityType = null;
        String recentActivityUserNickname = null;
        LocalDateTime recentActivityAt = null;

        if (latestPhoto.isPresent() &&
                (latestComment.isEmpty()
                        || latestPhoto.get().getCreatedAt().isAfter(latestComment.get().getCreatedAt()))) {

            MediaFile photo = latestPhoto.get();

            recentActivityType = "PHOTO";
            recentActivityUserNickname = photo.getUploader().getNickname();
            recentActivityAt = photo.getCreatedAt();

        } else if (latestComment.isPresent()) {

            MediaComment comment = latestComment.get();

            recentActivityType = "COMMENT";
            recentActivityUserNickname = comment.getUser().getNickname();
            recentActivityAt = comment.getCreatedAt();
        }

        return new WorkspaceInfoResponseDto(
                workspace.getId(),
                workspace.getName(),
                workspace.getExpiresAt(),
                workspace.getCreatedAt(),
                mediaFileRepository.existsByWorkspaceIdAndIsFinalTrue(workspace.getId()),
                members.size(),
                members.stream()
                        .map(member -> member.getUser().getId())
                        .toList(),
                memberProfiles,
                pendingInvitationUserIds,
                recentActivityType,
                recentActivityUserNickname,
                recentActivityAt
        );  // recentActivityAt;
    }

    public WorkspaceInfoResponseDto convertToInfoDto(
            Workspace workspace,
            Long userId) {

        List<WorkspaceInvitation> invitations =
                workspaceInvitationRepository.findAllByWorkspaceIdAndStatusIn(
                        workspace.getId(),
                        List.of(
                                InvitationStatus.ACCEPTED,
                                InvitationStatus.PENDING,
                                InvitationStatus.REJECTED
                        )
                );

        List<WorkspaceInvitationUserResponseDto> invitationUsers =
                invitations.stream()
                        .map(invitation -> {
                            User invitee = invitation.getInvitee();
                            User inviter = invitation.getInviter();

                            boolean isFriend =
                                    friendRepository.existsByUserAndFriendUser(
                                            userRepository.getReferenceById(userId),
                                            invitee
                                    );

                            boolean isInviter =
                                    inviter.getId().equals(userId);

                            return new WorkspaceInvitationUserResponseDto(
                                    invitation.getId(),
                                    invitee.getId(),
                                    invitee.getNickname(),
                                    profileImageUrlService.toResponseUrl(
                                            invitee.getProfileImageUrl()
                                    ),
                                    inviter.getId(),
                                    invitee.getId(),
                                    invitation.getStatus(),
                                    isFriend,
                                    isInviter
                            );
                        })
                        .sorted(
                                Comparator
                                        // 1차 정렬: A → B → C
                                        .comparingInt((WorkspaceInvitationUserResponseDto invitation) -> {
                                            if (invitation.isFriend() && invitation.isInviter()) {
                                                return 0; // A: 내 친구 + 내가 초대
                                            }

                                            if (invitation.isFriend() && !invitation.isInviter()) {
                                                return 1; // B: 내 친구 + 남이 초대
                                            }

                                            if (!invitation.isFriend() && !invitation.isInviter()) {
                                                return 2; // C: 비친구 + 남이 초대
                                            }

                                            return 3;
                                        })

                                        // 2차 정렬: ACCEPTED → PENDING → REJECTED
                                        .thenComparingInt(invitation -> {
                                            return switch (invitation.status()) {
                                                case ACCEPTED -> 0;
                                                case PENDING -> 1;
                                                case REJECTED -> 2;
                                            };
                                        })
                        )
                        .toList();

        WorkspaceInfoResponseDto baseDto = convertToInfoDto(workspace);

        return new WorkspaceInfoResponseDto(
                baseDto.id(),
                baseDto.name(),
                baseDto.expiresAt(),
                baseDto.createdAt(),
                baseDto.isFinal(),
                baseDto.memberCount(),
                baseDto.memberIds(),
                baseDto.memberProfiles(),
                baseDto.pendingInvitationUserIds(),
                baseDto.alreadyInvitedFriendIds(),
                invitationUsers,
                baseDto.recentActivityType(),
                baseDto.recentActivityUserNickname(),
                baseDto.recentActivityAt()
        );
    }

}
