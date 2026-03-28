package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 삭제 응답 DTO")
public record WorkspaceDeleteResponseDto(
        @Schema(description = "삭제된 워크스페이스 소유자 ID", example = "1")
        Long ownerId
) {
    public static WorkspaceDeleteResponseDto of(Long ownerId) {
        return new WorkspaceDeleteResponseDto(ownerId);
    }
}
