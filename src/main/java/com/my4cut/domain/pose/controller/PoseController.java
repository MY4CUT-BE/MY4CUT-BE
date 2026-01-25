package com.my4cut.domain.pose.controller;

import com.my4cut.domain.pose.dto.res.PoseResDto;
import com.my4cut.domain.pose.service.PoseService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/poses")
@RequiredArgsConstructor
public class PoseController {

    private final PoseService poseService;

    // 포즈 목록 조회
    @GetMapping
    public ApiResponse<List<PoseResDto.PoseListResDto>> getPoseList(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer peopleCount
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.getPoseList(sort, peopleCount)
        );
    }

    // 포즈 상세 조회
    @GetMapping("/{poseId}")
    public ApiResponse<PoseResDto.PoseDetailResDto> getPoseDetail(
            @PathVariable Long poseId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.getPoseDetail(poseId)
        );
    }

    // 포즈 즐겨찾기 등록
    @PostMapping("/{id}/bookmarks")
    public ApiResponse<PoseResDto.BookmarkResDto> addBookmark(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.addBookmark(userId, id)
        );
    }

    // 포즈 즐겨찾기 해제
    @DeleteMapping("/{id}/bookmarks")
    public ApiResponse<PoseResDto.BookmarkResDto> removeBookmark(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                poseService.removeBookmark(userId, id)
        );
    }
}
