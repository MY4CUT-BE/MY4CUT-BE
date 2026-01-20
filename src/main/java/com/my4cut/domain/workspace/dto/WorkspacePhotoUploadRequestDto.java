package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 워크스페이스 사진 업로드 요청 DTO.
 * 사진 파일과 촬영 날짜를 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspacePhotoUploadRequestDto {

    @Schema(description = "사진 파일")
    private MultipartFile file;

    @Schema(description = "사진 촬영 날짜 (YYYY-MM-DD)")
    private LocalDate takenDate;
}
