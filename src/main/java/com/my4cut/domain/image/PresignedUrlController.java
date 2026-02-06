package com.my4cut.global.image;

import com.my4cut.global.image.dto.PresignedUrlReqDto;
import com.my4cut.global.image.dto.PresignedUrlResDto;
import com.my4cut.global.image.dto.PresignedViewUrlReqDto;
import com.my4cut.global.image.dto.PresignedViewUrlResDto;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/images")
@Tag(name = "Image", description = "이미지 업로드/조회용 Presigned URL API")
public class PresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @Operation(
            summary = "이미지 업로드용 Presigned URL 발급",
            description = """
                    S3에 이미지를 직접 업로드하기 위한 PUT Presigned URL을 발급합니다.
                    서버는 실제 파일을 업로드하지 않으며, 클라이언트가 S3에 직접 업로드합니다.
                    파일 경로는 fileKey로만 관리하여 CloudFront 등 URL 변경에 유연하게 대응합니다.
                    """
    )
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResDto> createPresignedUrl(
            @AuthenticationPrincipal Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PresignedUrlReqDto.class)
                    )
            )
            @RequestBody @Valid PresignedUrlReqDto dto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                presignedUrlService.generate(userId, dto)
        );
    }

    @Operation(
            summary = "이미지 조회용 Presigned URL 발급(수정중!, 아마 사용 안할거 같습니다) ",
            description = """
                    이미 S3에 업로드된 이미지의 조회(GET)를 위한 Presigned URL을 발급합니다.
                    fileKey를 기반으로 임시 접근 URL을 생성합니다.
                    """
    )
    @PostMapping("/presigned-view-url")
    public ApiResponse<PresignedViewUrlResDto> createPresignedViewUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PresignedViewUrlReqDto.class)
                    )
            )
            @RequestBody @Valid PresignedViewUrlReqDto dto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                presignedUrlService.generateViewUrl(dto)
        );
    }
}