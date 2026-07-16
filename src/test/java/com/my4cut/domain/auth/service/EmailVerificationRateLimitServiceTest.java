package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.config.EmailVerificationRateLimitProperties;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.Invocation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

@SuppressWarnings({"unchecked", "rawtypes"})
class EmailVerificationRateLimitServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final EmailVerificationRateLimitService rateLimitService =
            new EmailVerificationRateLimitService(
                    redisTemplate,
                    new EmailVerificationRateLimitProperties()
            );

    @Test
    @DisplayName("발송 요청 제한: 요청자와 이메일 원문을 노출하지 않는 두 개의 제한 키를 사용한다")
    void checkSendAllowed_UsesHashedKeys() {
        String email = "User@example.com";
        String clientAddress = "203.0.113.10";
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .willReturn(1L);

        rateLimitService.checkSendAllowed(email, clientAddress);

        List<String> rateLimitKeys = mockingDetails(redisTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("execute"))
                .map(Invocation::getArguments)
                .map(arguments -> (List<String>) arguments[1])
                .flatMap(List::stream)
                .toList();

        assertThat(rateLimitKeys)
                .allMatch(key -> !key.contains(email) && !key.contains(clientAddress))
                .anyMatch(key -> key.startsWith("email:verify:rate:send:client:"))
                .anyMatch(key -> key.startsWith("email:verify:rate:send:target:"));
    }

    @Test
    @DisplayName("발송 요청 제한: 한도를 초과하면 429 비즈니스 예외를 반환한다")
    void checkSendAllowed_LimitExceeded_Fail() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .willReturn(0L);

        assertThatThrownBy(() ->
                rateLimitService.checkSendAllowed("user@example.com", "203.0.113.10")
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_RATE_LIMIT);
    }
}
