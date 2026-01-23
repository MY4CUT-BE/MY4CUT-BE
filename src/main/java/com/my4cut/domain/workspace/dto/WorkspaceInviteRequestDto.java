package com.my4cut.domain.workspace.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 멤버 초대 요청 DTO")
public record WorkspaceInviteRequestDto( @Schema(description = "워크스페이스 ID")
        Long workspaceId, @Schema(description = "초대할 유저 ID 리스트")
        List<Long> userIds) {
}
