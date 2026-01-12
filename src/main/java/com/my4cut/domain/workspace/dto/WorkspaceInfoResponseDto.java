package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 상세 정보 응답 DTO")
public record WorkspaceInfoResponseDto(
    @Schema(description = "워크스페이스 ID")
    Long id,
    @Schema(description = "워크스페이스 이름")
    String name,
    @Schema(description = "소유자 ID")
    Long ownerId,
    @Schema(description = "만료 일시")
    LocalDateTime expiresAt,
    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {}
