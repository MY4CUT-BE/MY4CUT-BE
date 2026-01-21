package com.my4cut.domain.auth.dto;

public class AuthReqDTO {

    public record RefreshDTO(
            String refreshToken
    ) {}
}
