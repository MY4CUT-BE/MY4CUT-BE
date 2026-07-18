package com.my4cut.domain.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 인증코드 발송 요청이다.
 */
public record EmailSendReqDto(
        @Schema(description = "인증코드를 받을 이메일 주소", example = "user@example.com")
        @NotBlank
        @Email
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
        String email
) {
}
