package com.my4cut.domain.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlReqDto(

        @NotNull
        ImageType type,

        @NotBlank
        String fileName,

        @NotBlank
        String contentType
) {
    public enum ImageType {
        PROFILE,
        CALENDAR
    }
}
