package com.my4cut.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserReqDTO {

    // 로그인
    public record LoginDTO(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    // 회원가입
    public record SignUpDTO(
            @Email
            @NotBlank
            @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
            String email,

            @Schema(description = "회원가입 이메일 인증 완료 후 발급받은 토큰")
            @NotBlank
            @Size(min = 43, max = 43, message = "올바른 이메일 인증 토큰이 아닙니다.")
            String verificationToken,

            @NotBlank
            @Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다.")
            String password,

            @NotBlank
            @Size(max = 10, message = "닉네임은 최대 10자입니다.")
            String nickname
    ) {}

    public record UpdateNicknameDTO(
            @NotBlank
            @Size(max = 10, message = "닉네임은 최대 10자입니다.")
            String nickname
    ) {}

}
