package com.my4cut.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
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
            @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
            String email,

            @Schema(description = "비밀번호 재설정 이메일 인증 완료 후 발급받은 토큰")
            @NotBlank
            @Size(min = 43, max = 43, message = "올바른 이메일 인증 토큰이 아닙니다.")
            String verificationToken,

            @NotBlank
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
            String newPassword
    ) {}
}
