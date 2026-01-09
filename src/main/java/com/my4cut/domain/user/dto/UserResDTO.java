package com.my4cut.domain.user.dto;

import lombok.Builder;

public class UserResDTO {
    // 로그인
    @Builder
    public record LoginDTO(
            Long memberId,
            String accessToken
    ){}
}
