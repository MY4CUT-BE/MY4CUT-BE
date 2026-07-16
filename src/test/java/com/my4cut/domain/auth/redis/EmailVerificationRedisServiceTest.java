package com.my4cut.domain.auth.redis;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.enums.EmailVerificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SuppressWarnings({"unchecked", "rawtypes"})
class EmailVerificationRedisServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final EmailVerificationRedisService redisService =
            new EmailVerificationRedisService(redisTemplate);

    @Test
    @DisplayName("쿨다운 키는 이메일 대소문자와 공백을 정규화하고 인증 목적을 포함한다")
    void acquireCooldown_NormalizesEmailAndIncludesPurpose() {
        String email = " User@Example.COM ";
        String expectedKey = "email:verify:signup:user@example.com:cooldown";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(expectedKey, "true", Duration.ofMinutes(1)))
                .willReturn(true);

        boolean acquired = redisService.acquireCooldown(email, EmailVerificationPurpose.SIGNUP);

        assertThat(acquired).isTrue();
        verify(valueOperations).setIfAbsent(expectedKey, "true", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("인증코드 검증은 목적별 키를 전달하고 원자 처리 결과를 변환한다")
    void verifyCode_UsesPurposeKeysAndMapsResult() {
        String email = "User@Example.COM";
        String code = "123456";
        String verificationTokenHash = "verification-token-hash";
        List<String> expectedKeys = List.of(
                "email:verify:password_reset:user@example.com:code",
                "email:verify:password_reset:user@example.com:fail",
                "email:verify:password_reset:user@example.com:verified",
                "email:verify:password_reset:user@example.com:cooldown"
        );
        given(redisTemplate.execute(
                any(org.springframework.data.redis.core.script.RedisScript.class),
                eq(expectedKeys),
                eq(code),
                eq("5"),
                eq(Long.toString(Duration.ofMinutes(30).toMillis())),
                eq(Long.toString(Duration.ofMinutes(5).toMillis())),
                eq(verificationTokenHash)
        )).willReturn(1L);

        EmailVerificationResult result = redisService.verifyCode(
                email,
                code,
                EmailVerificationPurpose.PASSWORD_RESET,
                verificationTokenHash
        );

        assertThat(result).isEqualTo(EmailVerificationResult.SUCCESS);
    }

    @Test
    @DisplayName("인증 완료 여부는 저장된 토큰 해시와 전달된 해시가 일치할 때만 참이다")
    void claimVerified_UsesPurposeKeysAndClaimId() {
        String email = "user@example.com";
        String tokenHash = "saved-token-hash";
        String claimId = "claim-id";
        List<String> expectedKeys = List.of(
                "email:verify:signup:user@example.com:verified",
                "email:verify:signup:user@example.com:claim"
        );
        given(redisTemplate.execute(
                any(org.springframework.data.redis.core.script.RedisScript.class),
                eq(expectedKeys),
                eq(tokenHash),
                eq(claimId),
                eq(Long.toString(Duration.ofMinutes(30).toMillis()))
        )).willReturn(1L);

        assertThat(redisService.claimVerified(
                email, EmailVerificationPurpose.SIGNUP, tokenHash, claimId
        )).isTrue();
    }
}
