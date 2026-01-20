package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspaceService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Member", description = "워크스페이스 멤버 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 나가기", description = "참여 중인 워크스페이스에서 나갑니다.")
    @DeleteMapping("/{workspaceId}/members/me")
    public ApiResponse<Void> leaveWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User user) {
        workspaceService.leaveWorkspace(workspaceId, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_LEAVE_SUCCESS);
    }
}
