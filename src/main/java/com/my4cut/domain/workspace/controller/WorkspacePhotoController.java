package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoUploadListRequestDto;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspacePhotoService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            @ModelAttribute WorkspacePhotoUploadListRequestDto requestDto,
            @AuthenticationPrincipal User user) {
        List<WorkspacePhotoResponseDto> result = workspacePhotoService.uploadPhotos(workspaceId, requestDto.photos(),
                user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.PHOTO_UPLOAD_SUCCESS, result);
    }
}
