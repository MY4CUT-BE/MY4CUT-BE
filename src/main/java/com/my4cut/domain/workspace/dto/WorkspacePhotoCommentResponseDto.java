package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "워크스페이스 사진 댓글 응답 DTO")
public record WorkspacePhotoCommentResponseDto(
        @Schema(description = "댓글 ID", example = "1") Long id,

        @Schema(description = "작성자 ID", example = "1") Long userId,

        @Schema(description = "작성자 닉네임", example = "홍길동") String nickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png") String profileImageUrl,

        @Schema(description = "댓글 내용", example = "우와 사진 잘 나왔네요!") String content,

        @Schema(description = "작성 일시", example = "2026-01-23T09:30:00") LocalDateTime createdAt) {
}
