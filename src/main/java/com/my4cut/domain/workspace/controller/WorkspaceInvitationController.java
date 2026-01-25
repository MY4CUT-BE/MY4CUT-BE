package com.my4cut.domain.workspace.controller;

import com.my4cut.domain.workspace.dto.WorkspaceInvitationResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.enums.WorkspaceSuccessCode;
import com.my4cut.domain.workspace.service.WorkspaceInvitationService;
import com.my4cut.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workspace Invitation", description = "워크스페이스 초대 관리 API")
@RestController
@RequestMapping("/workspaces/invitations")
@RequiredArgsConstructor
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;

    @Operation(summary = "멤버 초대", description = "워크스페이스에 새로운 멤버를 초대합니다. (초대장 발송)")
    @PostMapping
    public ApiResponse<Void> inviteMembers(
            @RequestBody WorkspaceInviteRequestDto dto,
            @AuthenticationPrincipal Long userId) {
        workspaceInvitationService.inviteMembers(dto, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_INVITE_SUCCESS);
    }

    @Operation(summary = "내가 받은 초대 목록 조회", description = "내가 참여 요청을 받은 워크스페이스 초대 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<List<WorkspaceInvitationResponseDto>> getMyInvitations(
            @AuthenticationPrincipal Long userId) {
        List<WorkspaceInvitationResponseDto> result = workspaceInvitationService.getMyInvitations(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_GET_SUCCESS, result);
    }

    @Operation(summary = "초대 수락", description = "워크스페이스 초대를 수락하여 멤버로 합류합니다.")
    @PostMapping("/{invitationId}/accept")
    public ApiResponse<Void> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal Long userId) {
        workspaceInvitationService.acceptInvitation(invitationId, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_INVITE_SUCCESS);
    }

    @Operation(summary = "초대 거절", description = "워크스페이스 초대를 거절합니다.")
    @PostMapping("/{invitationId}/reject")
    public ApiResponse<Void> rejectInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal Long userId) {
        workspaceInvitationService.rejectInvitation(invitationId, userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.WORKSPACE_INVITE_SUCCESS);
    }
}
