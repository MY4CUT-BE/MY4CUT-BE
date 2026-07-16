package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.enums.EmailVerificationResult;
import com.my4cut.domain.auth.redis.EmailVerificationRedisService;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/*
 * 이메일 인증 비즈니스 로직을 담당한다.
 *
 * - 인증코드 발송
 * - 인증코드 검증
 * - 인증 완료 여부 확인
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRedisService redisService;
    private final EmailVerificationCodeGenerator codeGenerator;
    private final EmailVerificationTokenGenerator tokenGenerator;
    private final EmailSenderService emailSenderService;

    /*
     * 인증코드를 생성하고 Redis 저장 후 이메일로 발송한다.
     */
    public void sendCode(String email, EmailVerificationPurpose purpose) {
        if (!redisService.acquireCooldown(email, purpose)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_COOLDOWN);
        }

        try {
            String code = codeGenerator.generate();
            redisService.saveCode(email, code, purpose);
            redisService.clearFailCount(email, purpose);
            emailSenderService.sendVerificationCode(email, code);
        } catch (EmailDeliveryUnknownException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // 코드 생성·저장·발송 중 명확한 실패가 발생하면 재요청을 막지 않도록 정리한다.
            clearCodeAndCooldownSafely(email, purpose, exception);
            throw exception;
        }
    }

    /*
     * Redis에 저장된 인증코드와 사용자가 입력한 코드를 비교한다.
     */
    public String verifyCode(String email, String inputCode, EmailVerificationPurpose purpose) {
        String verificationToken = tokenGenerator.generate();
        String verificationTokenHash = tokenGenerator.hash(verificationToken);
        EmailVerificationResult result = redisService.verifyCode(
                email,
                inputCode,
                purpose,
                verificationTokenHash
        );

        return switch (result) {
            case SUCCESS -> verificationToken;
            case CODE_NOT_FOUND -> throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_NOT_FOUND);
            case CODE_MISMATCH -> throw new BusinessException(ErrorCode.AUTH_EMAIL_CODE_MISMATCH);
            case FAIL_LIMIT_EXCEEDED ->
                    throw new BusinessException(ErrorCode.AUTH_EMAIL_VERIFY_FAIL_LIMIT);
        };
    }

    /*
     * 회원가입 직전 등에 이메일 인증 완료 여부를 확인할 때 사용한다.
     */
    public boolean claimVerifiedForTransaction(
            String email,
            EmailVerificationPurpose purpose,
            String verificationToken
    ) {
        String claimId = UUID.randomUUID().toString();
        boolean claimed = redisService.claimVerified(
                email,
                purpose,
                tokenGenerator.hash(verificationToken),
                claimId
        );
        if (!claimed) {
            return false;
        }

        registerClaimSynchronization(email, purpose, claimId);
        return true;
    }

    /*
     * DB 커밋 이후에만 verified 상태를 정리한다.
     */
    private void registerClaimSynchronization(
            String email,
            EmailVerificationPurpose purpose,
            String claimId
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisService.releaseClaim(email, purpose, claimId);
            throw new IllegalStateException("이메일 인증 토큰은 트랜잭션 안에서만 사용할 수 있습니다.");
        }

        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    try {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            redisService.completeClaim(email, purpose, claimId);
                        } else {
                            redisService.releaseClaim(email, purpose, claimId);
                        }
                    } catch (RuntimeException exception) {
                        log.error(
                                "이메일 인증 토큰 claim 정리 중 오류가 발생했습니다. purpose={}, committed={}",
                                purpose,
                                status == TransactionSynchronization.STATUS_COMMITTED,
                                exception
                        );
                    }
                }
            });
        } catch (RuntimeException exception) {
            redisService.releaseClaim(email, purpose, claimId);
            throw exception;
        }
    }

    /**
     * 실패 정리 중 Redis 오류가 추가로 발생해 최초 예외가 가려지지 않도록 보호한다.
     */
    private void clearCodeAndCooldownSafely(
            String email,
            EmailVerificationPurpose purpose,
            RuntimeException originalException
    ) {
        try {
            redisService.clearCodeAndCooldown(email, purpose);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.error(
                    "이메일 인증 실패 상태 정리 중 오류가 발생했습니다. purpose={}",
                    purpose,
                    cleanupException
            );
        }
    }
}
