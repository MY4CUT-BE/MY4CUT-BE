package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceUpdateRequestDto;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.service.WorkspaceService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Workspace", description = "워크스페이스 자체 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스를 생성합니다.")
    @PostMapping
    public ApiResponse<WorkspaceInfoResponseDto> createWorkspace(
            @RequestBody WorkspaceCreateRequestDto dto,
            @AuthenticationPrincipal User user) {
        WorkspaceInfoResponseDto result = workspaceService.createWorkspace(dto, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_CREATED, result);
    }

    @Operation(summary = "내 워크스페이스 목록 조회", description = "내가 참여 중인 워크스페이스 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<List<WorkspaceInfoResponseDto>> getMyWorkspaces(
            @AuthenticationPrincipal User user) {
        List<WorkspaceInfoResponseDto> result = workspaceService.getMyWorkspaces(user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_GET_SUCCESS, result);
    }

    @Operation(summary = "워크스페이스 상세 조회", description = "워크스페이스 상세 정보를 조회합니다.")
    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceInfoResponseDto> getWorkspaceInfo(
            @PathVariable Long workspaceId) {
        WorkspaceInfoResponseDto result = workspaceService.getWorkspaceInfo(workspaceId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_GET_SUCCESS, result);
    }

    @Operation(summary = "워크스페이스 수정", description = "워크스페이스 이름을 수정합니다.")
    @PatchMapping("/{workspaceId}")
    public ApiResponse<WorkspaceInfoResponseDto> updateWorkspace(
            @PathVariable Long workspaceId,
            @RequestBody WorkspaceUpdateRequestDto dto,
            @AuthenticationPrincipal User user) {
        WorkspaceInfoResponseDto result = workspaceService.updateWorkspace(workspaceId, dto, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_MODIFIED, result);
    }

    @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제(Soft Delete)합니다.")
    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Void> deleteWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User user) {
        workspaceService.deleteWorkspace(workspaceId, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_DELETED);
    }

    @ExceptionHandler(WorkspaceException.class)
    public ApiResponse<Void> handleWorkspaceException(WorkspaceException e) {
        return ApiResponse.onFailure(e.getErrorCode());
    }
}