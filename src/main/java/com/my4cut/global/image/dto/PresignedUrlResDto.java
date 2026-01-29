package com.my4cut.global.image.dto;

public record PresignedUrlResDto(
        String uploadUrl,
        String fileUrl
) {
}
