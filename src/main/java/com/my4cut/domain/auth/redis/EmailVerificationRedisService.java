package com.my4cut.domain.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
 * 이메일 인증에 필요한 Redis 저장소 접근을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationRedisService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofMinutes(1);
    private static final Duration FAIL_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    /*
     * 인증코드를 5분 동안 저장한다.
     */
    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(codeKey(email), code, CODE_TTL);
    }

    public String getCode(String email) {
        return redisTemplate.opsForValue().get(codeKey(email));
    }

    /*
     * 재전송 쿨다운을 1분 동안 유지한다.
     */
    public boolean acquireCooldown(String email) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(email), "true", COOLDOWN_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /*
     * 인증 실패 횟수는 첫 실패 시점부터 5분 동안 유지한다.
     */
    public long increaseFailCount(String email) {
        Long count = redisTemplate.opsForValue().increment(failKey(email));

        if (count != null && count == 1L) {
            redisTemplate.expire(failKey(email), FAIL_TTL);
        }

        return count == null ? 0L : count;
    }

    public long getFailCount(String email) {
        String value = redisTemplate.opsForValue().get(failKey(email));
        return value == null ? 0L : Long.parseLong(value);
    }

    public void clearFailCount(String email) {
        redisTemplate.delete(failKey(email));
    }

    /*
     * 인증 완료 상태는 30분 동안 유지한다.
     */
    public void markVerified(String email) {
        redisTemplate.opsForValue().set(verifiedKey(email), "true", VERIFIED_TTL);
    }

    public boolean isVerified(String email) {
        Boolean exists = redisTemplate.hasKey(verifiedKey(email));
        return Boolean.TRUE.equals(exists);
    }

    public void clearVerified(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    /*
     * 인증 성공 후 더 이상 필요 없는 키를 정리한다.
     */
    public void clearCodeAndFailInfo(String email) {
        redisTemplate.delete(codeKey(email));
        redisTemplate.delete(cooldownKey(email));
        redisTemplate.delete(failKey(email));
    }

    /*
     * 발송 실패 시 재시도를 막지 않도록 코드와 쿨다운만 정리한다.
     */
    public void clearCodeAndCooldown(String email) {
        redisTemplate.delete(codeKey(email));
        redisTemplate.delete(cooldownKey(email));
    }

    private String codeKey(String email) {
        return "email:verify:code:" + email;
    }

    private String cooldownKey(String email) {
        return "email:verify:cooldown:" + email;
    }

    private String failKey(String email) {
        return "email:verify:fail:" + email;
    }

    private String verifiedKey(String email) {
        return "email:verify:verified:" + email;
    }
}
