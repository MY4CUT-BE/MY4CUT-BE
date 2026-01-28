package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "워크스페이스 사진 업로드 요청 DTO")
public record WorkspacePhotoUploadRequestDto(
        @Schema(description = "업로드할 미디어 파일 id 리스트") List<Long> mediaIds) {
}
