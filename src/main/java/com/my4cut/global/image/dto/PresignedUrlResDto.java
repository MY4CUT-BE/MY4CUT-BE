package com.my4cut.global.image.dto;

public record PresignedUrlResDto(
        Long mediaId,
        String uploadUrl,
        String fileKey
) {
}
