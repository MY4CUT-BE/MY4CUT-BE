package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspaceInvitationService;
import com.my4cut.domain.workspace.service.WorkspaceMemberService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Workspace Member", description = "워크스페이스 멤버 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;
    private final WorkspaceInvitationService workspaceInvitationService;

    @Operation(summary = "워크스페이스 나가기", description = "워크스페이스에서 나갑니다.")
    @DeleteMapping("/{workspaceId}/members/me")
    public ApiResponse<Void> leaveWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User user) {
        workspaceMemberService.leaveWorkspace(workspaceId, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_LEAVE_SUCCESS); // SuccessCode 정의 필요할 수 있음
    }

    @Operation(summary = "멤버 초대", description = "워크스페이스에 새로운 멤버를 초대합니다. (초대장 발송)")
    @PostMapping("/{workspaceId}/invitations")
    public ApiResponse<Void> inviteMembers(
            @PathVariable Long workspaceId,
            @RequestBody WorkspaceInviteRequestDto dto,
            @AuthenticationPrincipal User user) {
        workspaceInvitationService.inviteMembers(workspaceId, dto, user.getId());
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_INVITE_SUCCESS);
    }

    // TODO: GET /{workspaceId}/members/active 추가 예정
}
