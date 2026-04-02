package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceDto {

    @Schema(description = "워크스페이스 생성 요청 DTO")
    public record CreateRequest(
            @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범")
            String name
    ) {}

    @Schema(description = "워크스페이스 수정 요청 DTO")
    public record UpdateRequest(
            @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범(수정)")
            String name
    ) {}

    @Schema(description = "워크스페이스 상세 정보 응답 DTO")
    public record InfoResponse(
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
            @Schema(description = "멤버 수")
            int memberCount,
            @Schema(description = "멤버 프로필 이미지 URL 리스트")
            List<String> memberProfiles,
            @Schema(description = "초대 대기(PENDING) 유저 ID 리스트")
            List<Long> pendingInvitationUserIds,
            @Schema(description = "현재 참여중인 멤버 유저 ID 리스트")
            List<Long> memberIds
    ) {}
}
