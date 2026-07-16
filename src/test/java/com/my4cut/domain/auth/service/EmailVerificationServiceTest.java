package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.enums.EmailVerificationResult;
import com.my4cut.domain.auth.redis.EmailVerificationRedisService;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final EmailVerificationPurpose PURPOSE = EmailVerificationPurpose.SIGNUP;

    private final EmailVerificationRedisService redisService = mock(EmailVerificationRedisService.class);
    private final EmailVerificationCodeGenerator codeGenerator = mock(EmailVerificationCodeGenerator.class);
    private final EmailVerificationTokenGenerator tokenGenerator = mock(EmailVerificationTokenGenerator.class);
    private final EmailSenderService emailSenderService = mock(EmailSenderService.class);
    private final EmailVerificationService emailVerificationService =
            new EmailVerificationService(
                    redisService,
                    codeGenerator,
                    tokenGenerator,
                    emailSenderService
            );

    @Test
    @DisplayName("이메일 인증코드 발송 성공: Redis 저장 후 EmailSender로 발송한다")
    void sendCode_Success() {
        String email = "test@example.com";
        String code = "123456";

        given(redisService.acquireCooldown(email, PURPOSE)).willReturn(true);
        given(codeGenerator.generate()).willReturn(code);

        emailVerificationService.sendCode(email, PURPOSE);

        verify(redisService).saveCode(email, code, PURPOSE);
        verify(redisService).clearFailCount(email, PURPOSE);
        verify(emailSenderService).sendVerificationCode(email, code);
        verify(redisService, never()).clearCodeAndCooldown(email, PURPOSE);
    }

    @Test
    @DisplayName("이메일 인증코드 발송 실패: Redis에 저장된 코드와 쿨다운을 정리한다")
    void sendCode_Fail_ClearsCodeAndCooldown() {
        String email = "test@example.com";
        String code = "123456";
        BusinessException sendFailure = new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED);

        given(redisService.acquireCooldown(email, PURPOSE)).willReturn(true);
        given(codeGenerator.generate()).willReturn(code);
        doThrow(sendFailure).when(emailSenderService).sendVerificationCode(email, code);

        assertThatThrownBy(() -> emailVerificationService.sendCode(email, PURPOSE))
                .isSameAs(sendFailure);

        verify(redisService).saveCode(email, code, PURPOSE);
        verify(redisService).clearFailCount(email, PURPOSE);
        verify(emailSenderService).sendVerificationCode(email, code);
        verify(redisService).clearCodeAndCooldown(email, PURPOSE);
    }

    @Test
    @DisplayName("이메일 발송 결과 불명: Redis 코드와 쿨다운을 정리하지 않는다")
    void sendCode_DeliveryUnknown_DoesNotClearCodeAndCooldown() {
        String email = "test@example.com";
        String code = "123456";
        EmailDeliveryUnknownException deliveryUnknown =
                new EmailDeliveryUnknownException(new RuntimeException("timeout"));

        given(redisService.acquireCooldown(email, PURPOSE)).willReturn(true);
        given(codeGenerator.generate()).willReturn(code);
        doThrow(deliveryUnknown).when(emailSenderService).sendVerificationCode(email, code);

        assertThatThrownBy(() -> emailVerificationService.sendCode(email, PURPOSE))
                .isSameAs(deliveryUnknown);

        verify(redisService).saveCode(email, code, PURPOSE);
        verify(redisService).clearFailCount(email, PURPOSE);
        verify(emailSenderService).sendVerificationCode(email, code);
        verify(redisService, never()).clearCodeAndCooldown(email, PURPOSE);
    }

    @Test
    @DisplayName("이메일 인증코드 발송 실패: 쿨다운 중에는 코드를 생성하거나 발송하지 않는다")
    void sendCode_Cooldown_Fail() {
        String email = "test@example.com";
        given(redisService.acquireCooldown(email, PURPOSE)).willReturn(false);

        assertThatThrownBy(() -> emailVerificationService.sendCode(email, PURPOSE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_CODE_COOLDOWN);

        verify(codeGenerator, never()).generate();
        verify(emailSenderService, never())
                .sendVerificationCode(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("이메일 인증코드 발송 실패: 실패 상태 정리 오류가 최초 예외를 가리지 않는다")
    void sendCode_CleanupFailure_PreservesOriginalException() {
        String email = "test@example.com";
        RuntimeException originalException = new RuntimeException("코드 생성 실패");
        RuntimeException cleanupException = new RuntimeException("Redis 정리 실패");
        given(redisService.acquireCooldown(email, PURPOSE)).willReturn(true);
        given(codeGenerator.generate()).willThrow(originalException);
        doThrow(cleanupException)
                .when(redisService)
                .clearCodeAndCooldown(email, PURPOSE);

        assertThatThrownBy(() -> emailVerificationService.sendCode(email, PURPOSE))
                .isSameAs(originalException)
                .satisfies(exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getSuppressed())
                                .containsExactly(cleanupException)
                );
    }

    @Test
    @DisplayName("이메일 인증코드 검증 성공: 요청 목적의 인증 상태만 저장한다")
    void verifyCode_Success_MarksPurposeVerified() {
        String email = "test@example.com";
        String code = "123456";
        String verificationToken = "verification-token";
        String verificationTokenHash = "verification-token-hash";
        given(tokenGenerator.generate()).willReturn(verificationToken);
        given(tokenGenerator.hash(verificationToken)).willReturn(verificationTokenHash);
        given(redisService.verifyCode(email, code, PURPOSE, verificationTokenHash))
                .willReturn(EmailVerificationResult.SUCCESS);

        String result = emailVerificationService.verifyCode(email, code, PURPOSE);

        assertThat(result).isEqualTo(verificationToken);
        verify(redisService).verifyCode(email, code, PURPOSE, verificationTokenHash);
    }

    @Test
    @DisplayName("이메일 인증코드 검증 실패: 다른 인증 목적에 저장된 코드는 사용할 수 없다")
    void verifyCode_DifferentPurpose_Fail() {
        String email = "test@example.com";
        String code = "123456";
        String verificationToken = "verification-token";
        String verificationTokenHash = "verification-token-hash";
        EmailVerificationPurpose resetPurpose = EmailVerificationPurpose.PASSWORD_RESET;
        given(tokenGenerator.generate()).willReturn(verificationToken);
        given(tokenGenerator.hash(verificationToken)).willReturn(verificationTokenHash);
        given(redisService.verifyCode(email, code, resetPurpose, verificationTokenHash))
                .willReturn(EmailVerificationResult.CODE_NOT_FOUND);

        assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code, resetPurpose))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_CODE_NOT_FOUND);

        verify(redisService).verifyCode(email, code, resetPurpose, verificationTokenHash);
    }

    @Test
    @DisplayName("이메일 인증코드 검증 실패: 실패 횟수 초과 결과를 전용 예외로 변환한다")
    void verifyCode_FailLimitExceeded_Fail() {
        String email = "test@example.com";
        String code = "123456";
        String verificationToken = "verification-token";
        String verificationTokenHash = "verification-token-hash";
        given(tokenGenerator.generate()).willReturn(verificationToken);
        given(tokenGenerator.hash(verificationToken)).willReturn(verificationTokenHash);
        given(redisService.verifyCode(email, code, PURPOSE, verificationTokenHash))
                .willReturn(EmailVerificationResult.FAIL_LIMIT_EXCEEDED);

        assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code, PURPOSE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_VERIFY_FAIL_LIMIT);
    }

    @Test
    @DisplayName("인증 토큰 claim은 DB 커밋 후 완료 처리한다")
    void claimVerified_Commit_CompletesClaim() {
        String email = "test@example.com";
        String token = "verification-token";
        String tokenHash = "verification-token-hash";
        given(tokenGenerator.hash(token)).willReturn(tokenHash);
        given(redisService.claimVerified(eq(email), eq(PURPOSE), eq(tokenHash), anyString()))
                .willReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(emailVerificationService.claimVerifiedForTransaction(email, PURPOSE, token))
                    .isTrue();

            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(redisService).completeClaim(eq(email), eq(PURPOSE), anyString());
            verify(redisService, never()).releaseClaim(eq(email), eq(PURPOSE), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("인증 토큰 claim은 DB 롤백 후 해제해 다시 사용할 수 있게 한다")
    void claimVerified_Rollback_ReleasesClaim() {
        String email = "test@example.com";
        String token = "verification-token";
        String tokenHash = "verification-token-hash";
        given(tokenGenerator.hash(token)).willReturn(tokenHash);
        given(redisService.claimVerified(eq(email), eq(PURPOSE), eq(tokenHash), anyString()))
                .willReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(emailVerificationService.claimVerifiedForTransaction(email, PURPOSE, token))
                    .isTrue();

            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(redisService).releaseClaim(eq(email), eq(PURPOSE), anyString());
            verify(redisService, never()).completeClaim(eq(email), eq(PURPOSE), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
