package com.my4cut.domain.workspace.dto;

import com.my4cut.domain.media.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WorkspacePhotoDto {

    @Schema(description = "워크스페이스 사진 업로드 요청 DTO")
    public record UploadRequest(
            @Schema(description = "업로드할 미디어 파일 id 리스트")
            List<Long> mediaIds
    ) {}

    @Schema(description = "워크스페이스 사진 업로드 응답 DTO")
    public record PhotoResponse(
            @Schema(description = "미디어 ID")
            Long mediaId,
            @Schema(description = "파일 키")
            String fileKey,
            @Schema(description = "실제 조회 가능한 URL (10분 유효)")
            String viewUrl,
            @Schema(description = "미디어 타입", example = "PHOTO")
            MediaType mediaType,
            @Schema(description = "사진 찍은 날짜")
            LocalDate takenDate,
            @Schema(description = "최종 확정 여부")
            Boolean isFinal,
            @Schema(description = "업로드 일시")
            LocalDateTime createdAt,
            @Schema(description = "업로더 닉네임")
            String uploaderNickname
    ) {}

    @Schema(description = "워크스페이스 사진 댓글 작성 요청 DTO")
    public record CommentRequest(
            @NotBlank(message = "댓글 내용은 필수입니다.")
            @Schema(description = "댓글 내용", example = "우와 사진 잘 나왔네요!")
            String content
    ) {}

    @Schema(description = "워크스페이스 사진 댓글 응답 DTO")
    public record CommentResponse(
            @Schema(description = "댓글 ID", example = "1")
            Long id,
            @Schema(description = "작성자 ID", example = "1")
            Long userId,
            @Schema(description = "작성자 닉네임", example = "홍길동")
            String nickname,
            @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png")
            String profileImageUrl,
            @Schema(description = "댓글 내용", example = "우와 사진 잘 나왔네요!")
            String content,
            @Schema(description = "작성 일시", example = "2026-01-23T09:30:00")
            LocalDateTime createdAt
    ) {}
}
