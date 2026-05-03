package com.my4cut.domain.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthReqDTO {

    public record RefreshDTO(
            String refreshToken
    ) {}

    public record KakaoLoginReqDto(
            String accessToken
    ) {}

    public record ResetPasswordReqDto(
            @NotBlank
            @Email
            String email,

            @NotBlank
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
            String newPassword
    ) {}
}
