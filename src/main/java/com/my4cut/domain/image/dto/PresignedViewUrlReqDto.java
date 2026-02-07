package com.my4cut.domain.image.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedViewUrlReqDto(

        @NotBlank
        String fileKey
) {
}
