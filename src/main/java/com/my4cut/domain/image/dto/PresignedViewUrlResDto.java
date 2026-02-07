package com.my4cut.domain.image.dto;

public record PresignedViewUrlResDto(
        String viewUrl,
        String fileKey
) {
}
