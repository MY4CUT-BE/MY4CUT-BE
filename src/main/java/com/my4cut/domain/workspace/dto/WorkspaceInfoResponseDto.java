package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Schema(description = "워크스페이스 상세 정보 응답 DTO")
public record WorkspaceInfoResponseDto(
    @Schema(description = "워크스페이스 ID")
    Long id,
    @Schema(description = "워크스페이스 이름")
    String name,
    @Schema(description = "소유자 ID")
    Long ownerId,
    @Schema(description = "만료 일시")
    LocalDateTime expiresAt,
    @Schema(description = "생성 일시")
    LocalDateTime createdAt,
    @Schema(description = "최종 확정 미디어 존재 여부")
    Boolean isFinal,
    @Schema(description = "멤버 수")
    int memberCount,
    @Schema(description = "참여 중인 멤버 userId 리스트")
    List<Long> memberIds,
    @Schema(description = "멤버 프로필 이미지 URL 리스트")
    List<String> memberProfiles,
    @Schema(description = "대기 중인 초대 userId 리스트")
    List<Long> pendingInvitationUserIds,
    @Schema(description = "이미 초대된 친구 userId 리스트 (수락 완료 멤버 + 대기 중인 초대, owner 제외)")
    List<Long> alreadyInvitedFriendIds
) {
    public WorkspaceInfoResponseDto(
            Long id,
            String name,
            Long ownerId,
            LocalDateTime expiresAt,
            LocalDateTime createdAt,
            Boolean isFinal,
            int memberCount,
            List<Long> memberIds,
            List<String> memberProfiles,
            List<Long> pendingInvitationUserIds) {
        this(
                id,
                name,
                ownerId,
                expiresAt,
                createdAt,
                isFinal,
                memberCount,
                memberIds,
                memberProfiles,
                pendingInvitationUserIds,
                buildAlreadyInvitedFriendIds(ownerId, memberIds, pendingInvitationUserIds));
    }

    private static List<Long> buildAlreadyInvitedFriendIds(
            Long ownerId,
            List<Long> memberIds,
            List<Long> pendingInvitationUserIds) {
        Set<Long> friendIds = new LinkedHashSet<>();

        if (memberIds != null) {
            memberIds.stream()
                    .filter(userId -> !Objects.equals(userId, ownerId))
                    .forEach(friendIds::add);
        }

        if (pendingInvitationUserIds != null) {
            pendingInvitationUserIds.stream()
                    .filter(userId -> !Objects.equals(userId, ownerId))
                    .forEach(friendIds::add);
        }

        return List.copyOf(friendIds);
    }
}
