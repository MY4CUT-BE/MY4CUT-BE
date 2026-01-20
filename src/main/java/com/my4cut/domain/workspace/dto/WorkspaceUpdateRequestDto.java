package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "워크스페이스 수정 요청 DTO")
public record WorkspaceUpdateRequestDto(
    @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범(수정)")
    String name
) {}
