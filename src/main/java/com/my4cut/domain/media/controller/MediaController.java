package com.my4cut.domain.media.controller;

import com.my4cut.domain.media.dto.res.MediaResDto;
import com.my4cut.domain.media.service.MediaService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    // 미디어 파일 업로드
    @PostMapping("/upload")
    public ApiResponse<MediaResDto.UploadResDto> uploadMedia(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.CREATED,
                mediaService.uploadMedia(userId, file)
        );
    }

    // 내 미디어 목록 조회
    @GetMapping
    public ApiResponse<List<MediaResDto.MediaListResDto>> getMyMediaList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.getMyMediaList(userId, page)
        );
    }

    // 미디어 상세 조회
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResDto.MediaDetailResDto> getMediaDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mediaId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.getMediaDetail(userId, mediaId)
        );
    }

    // 미디어 삭제
    @DeleteMapping("/{mediaId}")
    public ApiResponse<MediaResDto.DeleteResDto> deleteMedia(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long mediaId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.deleteMedia(userId, mediaId)
        );
    }
}
