package com.my4cut.global.image.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedViewUrlReqDto(

        @NotBlank
        String fileUrl
) {
}
