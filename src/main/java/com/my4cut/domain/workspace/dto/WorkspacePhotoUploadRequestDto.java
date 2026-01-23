package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Schema(description = "워크스페이스 사진 업로드 요청 DTO")
public record WorkspacePhotoUploadRequestDto(
        @Schema(description = "사진 파일") MultipartFile file,

        @Schema(description = "사진 촬영 날짜") LocalDate takenDate) {
}
