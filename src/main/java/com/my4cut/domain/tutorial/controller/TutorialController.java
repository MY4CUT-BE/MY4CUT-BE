package com.my4cut.domain.tutorial.controller;

import com.my4cut.domain.tutorial.dto.TutorialStatusResponseDto;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.tutorial.service.TutorialService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tutorial", description = "사용자별 홈, 워크스페이스, 사진 업로드 튜토리얼 진행 상태 관리 API")
@RestController
@RequestMapping("/tutorials")
@RequiredArgsConstructor
public class TutorialController {

    private final TutorialService tutorialService;

    @Operation(
            summary = "튜토리얼 상태 조회",
            description = "로그인한 사용자의 홈, 워크스페이스, 사진 업로드 튜토리얼 완료 여부를 조회합니다. "
                    + "상태 정보가 없는 기존 사용자는 모든 항목이 미완료(false)인 상태로 자동 생성됩니다."
    )
    @GetMapping
    public ApiResponse<TutorialStatusResponseDto> getStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(SuccessCode.OK, tutorialService.getStatus(userId));
    }

    @Operation(
            summary = "튜토리얼 완료 처리",
            description = "로그인한 사용자의 지정된 튜토리얼을 완료(true) 상태로 변경하고 전체 최신 상태를 반환합니다. "
                    + "이미 완료된 튜토리얼에 동일한 요청을 보내도 성공하며, 완료 상태는 미완료로 되돌리지 않습니다."
    )
    @PatchMapping("/{tutorialType}/complete")
    public ApiResponse<TutorialStatusResponseDto> complete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(
                    description = "완료할 튜토리얼 유형",
                    required = true,
                    example = "HOME",
                    schema = @Schema(allowableValues = {"HOME", "WORKSPACE", "PHOTO_UPLOAD"})
            )
            @PathVariable String tutorialType
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                tutorialService.complete(userId, TutorialType.from(tutorialType))
        );
    }
}
