package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.redis.EmailVerificationRedisService;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private final EmailVerificationRedisService redisService = mock(EmailVerificationRedisService.class);
    private final EmailVerificationCodeGenerator codeGenerator = mock(EmailVerificationCodeGenerator.class);
    private final EmailSenderService emailSenderService = mock(EmailSenderService.class);
    private final EmailVerificationService emailVerificationService =
            new EmailVerificationService(redisService, codeGenerator, emailSenderService);

    @Test
    @DisplayName("이메일 인증코드 발송 성공: Redis 저장 후 EmailSender로 발송한다")
    void sendCode_Success() {
        String email = "test@example.com";
        String code = "123456";

        given(redisService.acquireCooldown(email)).willReturn(true);
        given(codeGenerator.generate()).willReturn(code);

        emailVerificationService.sendCode(email);

        verify(redisService).saveCode(email, code);
        verify(redisService).clearFailCount(email);
        verify(emailSenderService).sendVerificationCode(email, code);
        verify(redisService, never()).clearCodeAndCooldown(email);
    }

    @Test
    @DisplayName("이메일 인증코드 발송 실패: Redis에 저장된 코드와 쿨다운을 정리한다")
    void sendCode_Fail_ClearsCodeAndCooldown() {
        String email = "test@example.com";
        String code = "123456";
        BusinessException sendFailure = new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED);

        given(redisService.acquireCooldown(email)).willReturn(true);
        given(codeGenerator.generate()).willReturn(code);
        doThrow(sendFailure).when(emailSenderService).sendVerificationCode(email, code);

        assertThatThrownBy(() -> emailVerificationService.sendCode(email))
                .isSameAs(sendFailure);

        verify(redisService).saveCode(email, code);
        verify(redisService).clearFailCount(email);
        verify(emailSenderService).sendVerificationCode(email, code);
        verify(redisService).clearCodeAndCooldown(email);
    }
}
