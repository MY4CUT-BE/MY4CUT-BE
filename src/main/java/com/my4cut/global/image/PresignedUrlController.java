package com.my4cut.global.image;

import com.my4cut.global.image.dto.PresignedUrlReqDto;
import com.my4cut.global.image.dto.PresignedUrlResDto;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/images")
public class PresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResDto> createPresignedUrl(
            @RequestBody @Valid PresignedUrlReqDto dto
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                presignedUrlService.generate(dto)
        );
    }
}
