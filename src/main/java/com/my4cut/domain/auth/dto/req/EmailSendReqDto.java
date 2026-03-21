package com.my4cut.domain.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * 이메일 인증코드 발송 요청 DTO
 *
 * - 사용자는 인증받고 싶은 이메일 주소를 전달한다.
 * - @NotBlank: 빈 값 방지
 * - @Email: 이메일 형식 검증
 */
public record EmailSendReqDto(
        @NotBlank
        @Email
        String email
) {
}