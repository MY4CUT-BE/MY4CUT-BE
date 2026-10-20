package com.my4cut.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "워크스페이스 생성 요청 DTO")
public record WorkspaceCreateRequestDto(
        @Schema(description = "워크스페이스 이름", example = "우리 가족 앨범")
        @NotBlank(message = "워크스페이스 이름은 비어 있을 수 없습니다.")
        @Size(max = 15, message = "워크스페이스 이름은 최대 15자입니다.")
        String name,
        @Schema(description = "초대할 유저 ID 리스트", example = "[21, 27, 73]")
        List<@NotNull(message = "초대할 사용자 ID는 null일 수 없습니다.") Long> userIds
) {}
