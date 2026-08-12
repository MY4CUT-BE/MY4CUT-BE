package com.my4cut.domain.tutorial.dto;

import com.my4cut.domain.tutorial.entity.UserTutorial;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자별 튜토리얼 완료 상태")
public record TutorialStatusResponseDto(
        @Schema(description = "홈 튜토리얼 완료 여부", example = "true")
        boolean home,
        @Schema(description = "워크스페이스 튜토리얼 완료 여부", example = "false")
        boolean workspace,
        @Schema(description = "사진 업로드 튜토리얼 완료 여부", example = "false")
        boolean photoUpload
) {
    public static TutorialStatusResponseDto from(UserTutorial tutorial) {
        return new TutorialStatusResponseDto(
                tutorial.isHomeCompleted(),
                tutorial.isWorkspaceCompleted(),
                tutorial.isPhotoUploadCompleted()
        );
    }
}
