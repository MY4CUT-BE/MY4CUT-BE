package com.my4cut.domain.workspace.dto;

import com.my4cut.domain.workspace.enums.InvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public class WorkspaceInvitationDto {

    @Schema(description = "워크스페이스 멤버 초대 요청 DTO")
    public record InviteRequest(
            @Schema(description = "워크스페이스 ID")
            Long workspaceId,
            @Schema(description = "초대할 유저 ID 리스트")
            List<Long> userIds
    ) {}

    @Schema(description = "워크스페이스 초대 응답 DTO")
    public record InvitationResponse(
            @Schema(description = "초대 ID")
            Long invitationId,
            @Schema(description = "워크스페이스 이름")
            String workspaceName,
            @Schema(description = "초대한 사람 닉네임")
            String inviterNickname,
            @Schema(description = "초대 상태")
            InvitationStatus status,
            @Schema(description = "초대 일시")
            LocalDateTime createdAt
    ) {}
}
