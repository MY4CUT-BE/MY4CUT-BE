package com.my4cut.domain.auth.dto.req;

public class AuthReqDTO {

    public record RefreshDTO(
            String refreshToken
    ) {}

    public record KakaoLoginReqDto(
            String accessToken
    ) {}
}
