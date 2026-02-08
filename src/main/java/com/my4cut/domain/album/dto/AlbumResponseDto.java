package com.my4cut.domain.album.dto;

import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class AlbumResponseDto {

    @Schema(description = "앨범 기본 정보 응답 DTO")
    public record Info(
            @Schema(description = "앨범 ID") Long id,
            @Schema(description = "앨범 이름") String name,
            @Schema(description = "앨범 내 미디어 개수") Integer mediaCount,
            @Schema(description = "대표 이미지 Key") String coverImageKey,
            @Schema(description = "대표 이미지 URL (10분 유효)") String coverImageUrl,
            @Schema(description = "생성 일시") LocalDateTime createdAt
    ) {}

    @Schema(description = "앨범 상세 정보 응답 DTO")
    public record Detail(
            @Schema(description = "앨범 ID") Long id,
            @Schema(description = "앨범 이름") String name,
            @Schema(description = "미디어 목록") List<WorkspacePhotoResponseDto> mediaList,
            @Schema(description = "생성 일시") LocalDateTime createdAt
    ) {}
}
