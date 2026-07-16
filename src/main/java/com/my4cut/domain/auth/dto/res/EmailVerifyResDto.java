package com.my4cut.domain.auth.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이메일 인증 완료 후 최종 회원가입 또는 비밀번호 재설정에 사용할 토큰 응답이다.
 */
public record EmailVerifyResDto(
        @Schema(
                description = "최종 회원가입 또는 비밀번호 재설정 요청에 포함할 이메일 인증 토큰",
                example = "Q1dERTIzNDU2Nzg5MGFiY2RlZg"
        )
        String verificationToken
) {
}
