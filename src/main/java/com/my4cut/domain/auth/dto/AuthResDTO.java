package com.my4cut.domain.auth.dto;

public class AuthResDTO {

    // 카카오 /v2/user/me 응답 중 필요한 것만
    public record KakaoUserResDto(
            Long id
    ) {}
}
