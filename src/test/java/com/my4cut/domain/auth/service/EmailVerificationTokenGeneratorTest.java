package com.my4cut.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenGeneratorTest {

    private final EmailVerificationTokenGenerator tokenGenerator =
            new EmailVerificationTokenGenerator();

    @Test
    @DisplayName("이메일 인증 토큰은 매번 다른 256비트 URL 안전 문자열로 생성된다")
    void generate_CreatesRandomUrlSafeToken() {
        String firstToken = tokenGenerator.generate();
        String secondToken = tokenGenerator.generate();

        assertThat(firstToken)
                .hasSize(43)
                .matches("^[A-Za-z0-9_-]+$")
                .isNotEqualTo(secondToken);
    }

    @Test
    @DisplayName("이메일 인증 토큰은 원문 대신 고정 길이 해시로 저장할 수 있다")
    void hash_ReturnsStableSha256Hash() {
        String token = "verification-token";

        assertThat(tokenGenerator.hash(token))
                .hasSize(64)
                .isEqualTo(tokenGenerator.hash(token));
    }
}
