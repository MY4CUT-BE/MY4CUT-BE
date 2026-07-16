package com.my4cut.domain.auth.enums;

/**
 * 이메일 인증의 사용 목적을 정의한다.
 * 서로 다른 인증 흐름의 코드와 인증 완료 상태가 섞이지 않도록 Redis 키를 구분할 때 사용한다.
 */
public enum EmailVerificationPurpose {
    SIGNUP,
    PASSWORD_RESET
}
