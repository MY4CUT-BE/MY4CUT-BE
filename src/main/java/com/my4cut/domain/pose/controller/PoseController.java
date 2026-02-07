package com.my4cut.domain.pose.controller;

import com.my4cut.domain.pose.dto.res.PoseResDto;
import com.my4cut.domain.pose.service.PoseService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/poses")
@RequiredArgsConstructor
@Tag(name = "Pose", description = "포즈 조회 및 즐겨찾기 API")
public class PoseController {

    private final PoseService poseService;

    @Operation(
            summary = "포즈 목록 조회",
            description = "포즈 목록을 조회합니다. 정렬 기준과 인원수로 필터링할 수 있습니다."
    )
    @GetMapping
    public ApiResponse<List<PoseResDto.PoseListResDto>> getPoseList(
            @Parameter(description = "정렬 기준") @RequestParam(required = false) String sort,
            @Parameter(description = "인원수 필터") @RequestParam(required = false) Integer peopleCount
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.getPoseList(sort, peopleCount)
        );
    }

    @Operation(
            summary = "포즈 상세 조회",
            description = "포즈 ID로 포즈 상세 정보를 조회합니다."
    )
    @GetMapping("/{poseId}")
    public ApiResponse<PoseResDto.PoseDetailResDto> getPoseDetail(
            @Parameter(description = "포즈 ID") @PathVariable Long poseId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.getPoseDetail(poseId)
        );
    }

    @Operation(
            summary = "포즈 즐겨찾기 등록",
            description = "포즈를 즐겨찾기에 등록합니다."
    )
    @PostMapping("/{id}/bookmarks")
    public ApiResponse<PoseResDto.BookmarkResDto> addBookmark(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "포즈 ID") @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.addBookmark(userId, id)
        );
    }

    @Operation(
            summary = "포즈 즐겨찾기 해제",
            description = "포즈를 즐겨찾기에서 해제합니다."
    )
    @DeleteMapping("/{id}/bookmarks")
    public ApiResponse<PoseResDto.BookmarkResDto> removeBookmark(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "포즈 ID") @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.removeBookmark(userId, id)
        );
    }
}
