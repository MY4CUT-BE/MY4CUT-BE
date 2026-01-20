package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 워크스페이스 사진 업로드 응답 DTO 필드.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspacePhotoResponseDto {

    @Schema(description = "사진 ID")
    private Long photoId;

    @Schema(description = "사진 URL")
    private String url;
}
