package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 생성 요청 DTO")
public record WorkspaceCreateRequestDto(
    @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범")
    String name
) {}
