package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "워크스페이스 사진 리스트 업로드 요청 DTO")
public record WorkspacePhotoUploadListRequestDto(
    @Schema(description = "업로드할 사진 리스트")
    List<WorkspacePhotoUploadRequestDto> photos
) {}

         
