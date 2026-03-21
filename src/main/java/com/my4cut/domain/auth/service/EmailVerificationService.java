package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.redis.EmailVerificationRedisService;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

/*
 * 이메일 인증 비즈니스 로직을 담당한다.
 *
 * - 인증코드 발송
 * - 인증코드 검증
 * - 인증 완료 여부 확인
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final long MAX_FAIL_COUNT = 5L;

    private final EmailVerificationRedisService redisService;
    private final EmailVerificationCodeGenerator codeGenerator;
    private final EmailSenderService emailSenderService;

    /*
     * 인증코드를 생성하고 Redis 저장 후 SES로 발송한다.
     */
    @Transactional
    public void sendCode(String email) {
        if (!redisService.acquireCooldown(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_COOLDOWN);
        }

        String code = codeGenerator.generate();

        redisService.saveCode(email, code);
        redisService.clearFailCount(email);

        try {
            emailSenderService.sendVerificationCode(email, code);
        } catch (RuntimeException exception) {
            // 발송 실패 시 저장된 코드와 쿨다운을 정리해 재요청이 가능하도록 맞춘다.
            redisService.clearCodeAndCooldown(email);
            throw exception;
        }
    }

    /*
     * Redis에 저장된 인증코드와 사용자가 입력한 코드를 비교한다.
     */
    @Transactional
    public void verifyCode(String email, String inputCode) {
        String savedCode = redisService.getCode(email);

        if (savedCode == null) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_NOT_FOUND);
        }

        if (redisService.getFailCount(email) >= MAX_FAIL_COUNT) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_VERIFY_FAIL_LIMIT);
        }

        if (!savedCode.equals(inputCode)) {
            long failCount = redisService.increaseFailCount(email);

            if (failCount >= MAX_FAIL_COUNT) {
                throw new BusinessException(ErrorCode.AUTH_EMAIL_VERIFY_FAIL_LIMIT);
            }

            throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_MISMATCH);
        }

        redisService.markVerified(email);
        redisService.clearCodeAndFailInfo(email);
    }

    /*
     * 회원가입 직전 등에 이메일 인증 완료 여부를 확인할 때 사용한다.
     */
    public boolean isVerified(String email) {
        return redisService.isVerified(email);
    }

    /*
     * 회원가입이 완료되면 verified 상태를 제거한다.
     */
    @Transactional
    public void clearVerified(String email) {
        redisService.clearVerified(email);
    }

    /*
     * DB 커밋 이후에만 verified 상태를 정리한다.
     */
    public void clearVerifiedAfterCommit(String email) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisService.clearVerified(email);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisService.clearVerified(email);
            }
        });
    }
}
