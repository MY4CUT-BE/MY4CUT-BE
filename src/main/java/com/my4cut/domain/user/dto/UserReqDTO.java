package com.my4cut.domain.user.dto;

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
            @Email @NotBlank String email,

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

    public record UpdateProfileImageDTO(
            @NotBlank
            String profileImageUrl
    ) {}
}
