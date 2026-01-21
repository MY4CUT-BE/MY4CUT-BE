package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 사진 업로드 응답 DTO")
public record WorkspacePhotoResponseDto(
        @Schema(description = "사진 ID") Long photoId,

        @Schema(description = "사진 URL") String url) {
}
