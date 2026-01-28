package com.my4cut.domain.day4cut.controller;

import com.my4cut.domain.day4cut.dto.req.Day4CutReqDto;
import com.my4cut.domain.day4cut.dto.res.Day4CutResDto;
import com.my4cut.domain.day4cut.exception.Day4CutException;
import com.my4cut.domain.day4cut.service.Day4CutService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 하루네컷 관련 API를 제공하는 컨트롤러.
 */
@Tag(name = "Day4Cut", description = "하루네컷 API - 하루네컷 생성, 조회, 수정, 삭제 기능을 제공합니다.")
@RestController
@RequestMapping("/day4cut")
@RequiredArgsConstructor
public class Day4CutController {

    private final Day4CutService day4CutService;

    @Operation(
            summary = "하루네컷 생성",
            description = "새로운 하루네컷을 생성합니다. 이미지(최소 1장, 썸네일 지정 필수), 글(필수), 이모티콘(선택)으로 구성됩니다. " +
                    "이미지가 1장인 경우 자동으로 썸네일로 지정됩니다."
    )
    @PostMapping
    public ApiResponse<Day4CutResDto.CreateResDto> createDay4Cut(
            @AuthenticationPrincipal Long userId,
            @RequestBody Day4CutReqDto.CreateReqDto reqDto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.CREATED,
                day4CutService.createDay4Cut(userId, reqDto)
        );
    }

    @Operation(
            summary = "하루네컷 조회",
            description = "특정 하루네컷의 상세 정보를 조회합니다. 본인이 작성한 하루네컷만 조회할 수 있습니다."
    )
    @GetMapping
    public ApiResponse<Day4CutResDto.DetailResDto> getDay4Cut(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 하루네컷 ID", required = true)
            @RequestParam Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                day4CutService.getDay4Cut(userId, id)
        );
    }

    @Operation(
            summary = "하루네컷 수정",
            description = "하루네컷의 내용, 이모티콘, 이미지를 수정합니다. " +
                    "수정 시 이미지는 전체 교체 방식으로 처리됩니다. " +
                    "이모티콘은 추가/제거(null 전달)가 가능합니다. 본인이 작성한 하루네컷만 수정할 수 있습니다."
    )
    @PatchMapping
    public ApiResponse<Day4CutResDto.UpdateResDto> updateDay4Cut(
            @AuthenticationPrincipal Long userId,
            @RequestBody Day4CutReqDto.UpdateReqDto reqDto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                day4CutService.updateDay4Cut(userId, reqDto)
        );
    }

    @Operation(
            summary = "하루네컷 삭제",
            description = "하루네컷을 삭제합니다. 본인이 작성한 하루네컷만 삭제할 수 있습니다."
    )
    @DeleteMapping
    public ApiResponse<Day4CutResDto.DeleteResDto> deleteDay4Cut(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 하루네컷 ID", required = true)
            @RequestParam Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                day4CutService.deleteDay4Cut(userId, id)
        );
    }

    // 하루네컷이 존재하는 날짜 목록 조회
    @GetMapping("/calendar")
    public ApiResponse<Day4CutResDto.CalendarResDto> getCalendar(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                day4CutService.getCalendar(userId, year, month)
        );
    }

    @ExceptionHandler(Day4CutException.class)
    public ApiResponse<Void> handleDay4CutException(Day4CutException e) {
        return ApiResponse.onFailure(e.getErrorCode());
    }
}
