package com.my4cut.domain.workspace.dto;

import com.my4cut.domain.media.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 사진 업로드 응답 DTO")
public record WorkspacePhotoResponseDto(
        @Schema(description = "미디어 ID") Long mediaId,

        @Schema(description = "파일 키") String fileKey,

        @Schema(description = "실제 조회 가능한 URL (10분 유효)") String viewUrl,

        @Schema(description = "미디어 타입", example = "PHOTO") MediaType mediaType,

        @Schema(description = "사진 찍은 날짜") LocalDate takenDate,

        @Schema(description = "최종 확정 여부") Boolean isFinal,

        @Schema(description = "업로드 일시") LocalDateTime createdAt,

        @Schema(description = "업로더 닉네임") String uploaderNickname
) {
}
