package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "워크스페이스 수정 요청 DTO")
public record WorkspaceUpdateRequestDto(
    @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범(수정)")
    @NotBlank(message = "워크스페이스 이름은 비어 있을 수 없습니다.")
    @Size(max = 15, message = "워크스페이스 이름은 최대 15자입니다.")
    String name
) {}
