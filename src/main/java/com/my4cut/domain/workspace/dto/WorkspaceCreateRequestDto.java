package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 생성 요청 DTO")
public record WorkspaceCreateRequestDto(
    @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범")
    String name,
    @Schema(description = "만료 일시 (선택)", example = "2026-12-31T23:59:59")
    LocalDateTime expiresAt
) {}
