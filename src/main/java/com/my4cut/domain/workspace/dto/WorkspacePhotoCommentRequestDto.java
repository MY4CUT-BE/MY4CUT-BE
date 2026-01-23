package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "워크스페이스 사진 댓글 작성 요청 DTO")
public record WorkspacePhotoCommentRequestDto(
        @NotBlank(message = "댓글 내용은 필수입니다.") @Schema(description = "댓글 내용", example = "우와 사진 잘 나왔네요!") String content) {
}
