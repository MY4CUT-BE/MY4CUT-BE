package com.my4cut.domain.workspace.dto;

import com.my4cut.domain.workspace.enums.InvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 초대 사용자 정보")
public record WorkspaceInvitationUserResponseDto(

        @Schema(description = "초대 ID")
        Long invitationId,

        @Schema(description = "사용자 ID")
        Long userId,

        @Schema(description = "사용자 닉네임")
        String nickname,

        @Schema(description = "사용자 프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "초대한 사람 ID")
        Long inviterId,

        @Schema(description = "초대받은 사람 ID")
        Long inviteeId,

        @Schema(description = "초대 상태")
        InvitationStatus status,

        @Schema(description = "현재 사용자와 친구인지 여부")
        Boolean isFriend,

        @Schema(description = "현재 사용자가 초대한 사람인지 여부")
        Boolean isInviter
) {
}