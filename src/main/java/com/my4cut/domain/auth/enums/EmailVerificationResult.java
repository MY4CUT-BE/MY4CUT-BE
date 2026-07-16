package com.my4cut.domain.auth.enums;

/**
 * Redis에서 원자적으로 수행한 이메일 인증코드 검증 결과를 나타낸다.
 */
public enum EmailVerificationResult {
    SUCCESS,
    CODE_NOT_FOUND,
    CODE_MISMATCH,
    FAIL_LIMIT_EXCEEDED
}
