package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspacePhotoService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Workspace Photo", description = "워크스페이스 사진 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspacePhotoController {

    private final WorkspacePhotoService workspacePhotoService;

    @Operation(summary = "사진 업로드", description = "워크스페이스에 여러 장의 사진을 업로드합니다.")
    @PostMapping("/{workspaceId}/photos")
    public ApiResponse<List<WorkspacePhotoResponseDto>> uploadPhotos(
            @PathVariable Long workspaceId,
            @RequestPart("files") MultipartFile[] files,
            @AuthenticationPrincipal User user) {
        List<WorkspacePhotoResponseDto> result = workspacePhotoService.uploadPhotos(workspaceId, files, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.PHOTO_UPLOAD_SUCCESS, result);
    }
}
