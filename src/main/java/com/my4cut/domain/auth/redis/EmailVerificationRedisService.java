package com.my4cut.domain.auth.redis;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.enums.EmailVerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.List;

/*
 * 이메일 인증에 필요한 Redis 저장소 접근을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationRedisService {

    private static final long MAX_FAIL_COUNT = 5L;

    private static final DefaultRedisScript<Long> VERIFY_CODE_SCRIPT = new DefaultRedisScript<>(
            """
                    local savedCode = redis.call('GET', KEYS[1])
                    if not savedCode then
                        return 0
                    end

                    local failCount = tonumber(redis.call('GET', KEYS[2]) or '0')
                    if failCount >= tonumber(ARGV[2]) then
                        return -2
                    end

                    if savedCode ~= ARGV[1] then
                        local increasedCount = redis.call('INCR', KEYS[2])
                        if increasedCount == 1 then
                            redis.call('PEXPIRE', KEYS[2], ARGV[4])
                        end
                        if increasedCount >= tonumber(ARGV[2]) then
                            return -2
                        end
                        return -1
                    end

                    redis.call('SET', KEYS[3], ARGV[5], 'PX', ARGV[3])
                    redis.call('DEL', KEYS[1], KEYS[2], KEYS[4])
                    return 1
                    """,
            Long.class
    );

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);
    private static final Duration FAIL_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    /*
     * 인증코드를 5분 동안 저장한다.
     */
    public void saveCode(String email, String code, EmailVerificationPurpose purpose) {
        redisTemplate.opsForValue().set(codeKey(email, purpose), code, CODE_TTL);
    }

    /*
     * 재전송 쿨다운을 1분 동안 유지한다.
     */
    public boolean acquireCooldown(String email, EmailVerificationPurpose purpose) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(email, purpose), "true", COOLDOWN_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /*
     * 인증 실패 횟수는 첫 실패 시점부터 5분 동안 유지한다.
     */
    public void clearFailCount(String email, EmailVerificationPurpose purpose) {
        redisTemplate.delete(failKey(email, purpose));
    }

    /*
     * 인증 완료 상태는 30분 동안 유지한다.
     */
    public boolean isVerified(
            String email,
            EmailVerificationPurpose purpose,
            String verificationTokenHash
    ) {
        String savedTokenHash = redisTemplate.opsForValue().get(verifiedKey(email, purpose));
        if (savedTokenHash == null) {
            return false;
        }

        return MessageDigest.isEqual(
                savedTokenHash.getBytes(StandardCharsets.UTF_8),
                verificationTokenHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    public void clearVerified(String email, EmailVerificationPurpose purpose) {
        redisTemplate.delete(verifiedKey(email, purpose));
    }

    /*
     * 인증 성공 후 더 이상 필요 없는 키를 정리한다.
     */
    public EmailVerificationResult verifyCode(
            String email,
            String inputCode,
            EmailVerificationPurpose purpose,
            String verificationTokenHash
    ) {
        Long result = redisTemplate.execute(
                VERIFY_CODE_SCRIPT,
                List.of(
                        codeKey(email, purpose),
                        failKey(email, purpose),
                        verifiedKey(email, purpose),
                        cooldownKey(email, purpose)
                ),
                inputCode,
                Long.toString(MAX_FAIL_COUNT),
                Long.toString(VERIFIED_TTL.toMillis()),
                Long.toString(FAIL_TTL.toMillis()),
                verificationTokenHash
        );

        if (result == null) {
            throw new IllegalStateException("Redis 이메일 인증코드 검증 결과가 없습니다.");
        }

        return switch (result.intValue()) {
            case 1 -> EmailVerificationResult.SUCCESS;
            case 0 -> EmailVerificationResult.CODE_NOT_FOUND;
            case -1 -> EmailVerificationResult.CODE_MISMATCH;
            case -2 -> EmailVerificationResult.FAIL_LIMIT_EXCEEDED;
            default -> throw new IllegalStateException("알 수 없는 Redis 이메일 인증 결과입니다: " + result);
        };
    }

    /*
     * 발송 실패 시 재시도를 막지 않도록 코드와 쿨다운만 정리한다.
     */
    public void clearCodeAndCooldown(String email, EmailVerificationPurpose purpose) {
        redisTemplate.delete(codeKey(email, purpose));
        redisTemplate.delete(cooldownKey(email, purpose));
    }

    private String codeKey(String email, EmailVerificationPurpose purpose) {
        return keyPrefix(email, purpose) + ":code";
    }

    private String cooldownKey(String email, EmailVerificationPurpose purpose) {
        return keyPrefix(email, purpose) + ":cooldown";
    }

    private String failKey(String email, EmailVerificationPurpose purpose) {
        return keyPrefix(email, purpose) + ":fail";
    }

    private String verifiedKey(String email, EmailVerificationPurpose purpose) {
        return keyPrefix(email, purpose) + ":verified";
    }

    /**
     * 이메일 대소문자를 바꿔 쿨다운과 인증 상태를 우회하지 못하도록 Redis 키를 정규화한다.
     */
    private String keyPrefix(String email, EmailVerificationPurpose purpose) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedPurpose = purpose.name().toLowerCase(Locale.ROOT);
        return "email:verify:" + normalizedPurpose + ":" + normalizedEmail;
    }
}
