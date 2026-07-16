package com.my4cut.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이메일 인증코드 검증 요청이다.
 */
public record EmailVerifyReqDto(
        @Schema(description = "인증코드를 발급받은 이메일 주소", example = "user@example.com")
        @NotBlank
        @Email
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
        String email,

        @Schema(description = "이메일로 발송된 6자리 인증코드", example = "123456")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "인증코드는 6자리 숫자여야 합니다.")
        String code
) {
}
