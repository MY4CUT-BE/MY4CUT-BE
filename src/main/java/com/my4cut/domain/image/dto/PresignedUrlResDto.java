package com.my4cut.domain.image.dto;

public record PresignedUrlResDto(
        Long mediaId,
        String uploadUrl,
        String fileKey
) {
}
