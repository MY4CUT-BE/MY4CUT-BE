package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.config.EmailVerificationRateLimitProperties;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * 공개 이메일 인증 API의 대량 호출을 제한한다.
 * 개인정보가 Redis 키에 직접 남지 않도록 이메일과 요청자 주소는 SHA-256 해시로 저장한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationRateLimitService {

    private static final DefaultRedisScript<Long> ACQUIRE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[2])
                    end
                    if count > tonumber(ARGV[1]) then
                        return 0
                    end
                    return 1
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationRateLimitProperties properties;

    /**
     * 요청자별 단기 한도와 이메일별 시간당 한도를 모두 적용한다.
     */
    public void checkSendAllowed(String email, String clientAddress) {
        acquireOrThrow(
                "email:verify:rate:send:client:" + hash(normalizeClientAddress(clientAddress)),
                properties.getSendClient()
        );
        acquireOrThrow(
                "email:verify:rate:send:target:" + hash(email.trim().toLowerCase(Locale.ROOT)),
                properties.getSendEmail()
        );
    }

    /**
     * 여러 이메일을 대상으로 한 인증코드 대입 공격을 요청자 기준으로 제한한다.
     */
    public void checkVerifyAllowed(String clientAddress) {
        acquireOrThrow(
                "email:verify:rate:verify:client:" + hash(normalizeClientAddress(clientAddress)),
                properties.getVerifyClient()
        );
    }

    private void acquireOrThrow(
            String key,
            EmailVerificationRateLimitProperties.Limit limit
    ) {
        Long allowed = redisTemplate.execute(
                ACQUIRE_LIMIT_SCRIPT,
                List.of(key),
                Long.toString(limit.getMaxAttempts()),
                Long.toString(limit.getWindow().toMillis())
        );

        if (!Long.valueOf(1L).equals(allowed)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_RATE_LIMIT);
        }
    }

    private String normalizeClientAddress(String clientAddress) {
        return clientAddress == null || clientAddress.isBlank()
                ? "unknown"
                : clientAddress.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
