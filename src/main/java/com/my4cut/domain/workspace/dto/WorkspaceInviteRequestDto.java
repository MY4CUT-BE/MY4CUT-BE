package com.my4cut.domain.workspace.dto;

import java.util.List;

/**
 * 워크스페이스 멤버 초대를 위한 요청 DTO.
 */
public record WorkspaceInviteRequestDto(
        List<Long> userIds) {
}
