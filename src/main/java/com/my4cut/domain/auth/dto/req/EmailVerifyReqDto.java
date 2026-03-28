package com.my4cut.domain.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/*
 * 이메일 인증코드 검증 요청 DTO다.
 */
public record EmailVerifyReqDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "인증코드는 6자리 숫자여야 합니다.")
        String code
) {
}
