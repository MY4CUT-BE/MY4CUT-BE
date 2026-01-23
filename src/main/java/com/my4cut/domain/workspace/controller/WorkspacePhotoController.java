package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.workspace.dto.*;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspacePhotoService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workspace Photo", description = "워크스페이스 사진 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspacePhotoController {

    private final WorkspacePhotoService workspacePhotoService;

    @Operation(summary = "사진 업로드", description = "워크스페이스에 여러 장의 사진과 메타데이터(날짜 등)를 업로드합니다.")
    @PostMapping(value = "/{workspaceId}/photos", consumes = "multipart/form-data")
    public ApiResponse<List<WorkspacePhotoResponseDto>> uploadPhotos(
            @PathVariable Long workspaceId,
            @Valid @ModelAttribute WorkspacePhotoUploadListRequestDto requestDto,
            @AuthenticationPrincipal Long userId) {
        List<WorkspacePhotoResponseDto> result = workspacePhotoService.uploadPhotos(workspaceId, requestDto.photos(),
                userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.PHOTO_UPLOAD_SUCCESS, result);
    }

    @Operation(summary = "사진 목록 조회", description = "워크스페이스의 사진 목록을 조회합니다.")
    @GetMapping("/{workspaceId}/photos")
    public ApiResponse<List<WorkspacePhotoResponseDto>> getPhotos(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "정렬 순서", schema = @Schema(allowableValues = { "latest",
                    "oldest" })) @RequestParam(name = "sort", defaultValue = "latest") String sort) {
        List<WorkspacePhotoResponseDto> result = workspacePhotoService.getPhotos(workspaceId, sort, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.PHOTO_LIST_GET_SUCCESS, result);
    }

    @Operation(summary = "사진 삭제", description = "워크스페이스의 특정 사진을 삭제합니다.")
    @DeleteMapping("/{workspaceId}/photos/{id}")
    public ApiResponse<Void> deletePhoto(
            @PathVariable Long workspaceId,
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        workspacePhotoService.deletePhoto(workspaceId, id, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.PHOTO_DELETE_SUCCESS, null);
    }

    @Operation(summary = "댓글 목록 조회", description = "워크스페이스 특정 사진의 댓글 목록을 조회합니다.")
    @GetMapping("/{workspaceId}/photos/{photoId}/comments")
    public ApiResponse<List<WorkspacePhotoCommentResponseDto>> getComments(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal Long userId) {
        List<WorkspacePhotoCommentResponseDto> result = workspacePhotoService.getComments(workspaceId, photoId,
                userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.COMMENT_LIST_GET_SUCCESS, result);
    }

    @Operation(summary = "댓글 등록", description = "워크스페이스 특정 사진에 댓글을 등록합니다.")
    @PostMapping("/{workspaceId}/photos/{photoId}/comments")
    public ApiResponse<Void> createComment(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId,
            @Valid @RequestBody WorkspacePhotoCommentRequestDto requestDto,
            @AuthenticationPrincipal Long userId) {
        workspacePhotoService.createComment(workspaceId, photoId, requestDto, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.COMMENT_CREATE_SUCCESS);
    }

    @Operation(summary = "댓글 삭제", description = "워크스페이스 특정 사진의 댓글을 삭제합니다.")
    @DeleteMapping("/{workspaceId}/photos/{photoId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long workspaceId,
            @PathVariable Long photoId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        workspacePhotoService.deleteComment(workspaceId, photoId, commentId, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.COMMENT_DELETE_SUCCESS);
    }
}
