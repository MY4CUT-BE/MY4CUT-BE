package com.my4cut.domain.media.controller;

import com.my4cut.domain.media.dto.res.MediaResDto;
import com.my4cut.domain.media.service.MediaService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "미디어 업로드/조회/삭제 API")
public class MediaController {

    private final MediaService mediaService;

    @Operation(
            summary = "미디어 파일 업로드",
            description = "미디어 파일 1개를 업로드합니다."
    )
    @PostMapping("/upload")
    public ApiResponse<MediaResDto.UploadResDto> uploadMedia(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 미디어 파일") @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.CREATED,
                mediaService.uploadMedia(userId, file)
        );
    }

    @Operation(
            summary = "미디어 파일 다건 업로드",
            description = "multipart/form-data로 미디어 파일 여러 개를 서버로 업로드합니다. 5MB × 10장 기준입니다."
    )
    @PostMapping(
            value = "/upload/bulk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<List<MediaResDto.UploadResDto>> uploadMediaBulk(
            @AuthenticationPrincipal Long userId,

            @RequestPart("files")
            @Parameter(
                    description = "업로드할 미디어 파일 목록",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                                            type = "string",
                                            format = "binary"
                                    )
                            )
                    )
            )
            List<MultipartFile> files
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.CREATED,
                mediaService.uploadMediaBulk(userId, files)
        );
    }

    @Operation(
            summary = "내 미디어 목록 조회",
            description = "현재 로그인한 사용자의 미디어 목록을 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<MediaResDto.MediaListResDto>> getMyMediaList(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.getMyMediaList(userId, page)
        );
    }

    @Operation(
            summary = "미디어 상세 조회",
            description = "미디어 ID로 미디어 상세 정보를 조회합니다."
    )
    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResDto.MediaDetailResDto> getMediaDetail(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "미디어 ID") @PathVariable Long mediaId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.getMediaDetail(userId, mediaId)
        );
    }

    @Operation(
            summary = "미디어 삭제",
            description = "미디어 ID로 미디어를 삭제합니다."
    )
    @DeleteMapping("/{mediaId}")
    public ApiResponse<MediaResDto.DeleteResDto> deleteMedia(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "미디어 ID") @PathVariable Long mediaId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                mediaService.deleteMedia(userId, mediaId)
        );
    }
}
