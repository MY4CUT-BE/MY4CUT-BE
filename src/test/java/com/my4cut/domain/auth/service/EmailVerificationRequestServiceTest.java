package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailVerificationRequestServiceTest {

    private static final String CLIENT_ADDRESS = "127.0.0.1";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    private final EmailVerificationRateLimitService rateLimitService =
            mock(EmailVerificationRateLimitService.class);
    private final EmailVerificationRequestService requestService =
            new EmailVerificationRequestService(userRepository, emailVerificationService, rateLimitService);

    @Test
    @DisplayName("회원가입 인증코드 발송 성공: 신규 이메일은 발송한다")
    void sendSignupCode_NewEmail_Success() {
        String email = "new@example.com";
        given(userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED))
                .willReturn(false);

        requestService.sendSignupCode(email, CLIENT_ADDRESS);

        verify(rateLimitService).checkSendAllowed(email, CLIENT_ADDRESS);
        verify(emailVerificationService).sendCode(email, EmailVerificationPurpose.SIGNUP);
    }

    @Test
    @DisplayName("회원가입 인증코드 발송 실패: 탈퇴하지 않은 기존 이메일은 차단한다")
    void sendSignupCode_ExistingEmail_Fail() {
        String email = "member@example.com";
        given(userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED))
                .willReturn(true);

        assertThatThrownBy(() -> requestService.sendSignupCode(email, CLIENT_ADDRESS))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_DUPLICATE_EMAIL);

        verify(emailVerificationService, never())
                .sendCode(email, EmailVerificationPurpose.SIGNUP);
    }

    @Test
    @DisplayName("회원가입 인증코드 발송 성공: 탈퇴 이메일은 재가입을 위해 발송한다")
    void sendSignupCode_DeletedEmail_Success() {
        String email = "deleted@example.com";
        given(userRepository.existsByEmailAndStatusNot(email, UserStatus.DELETED))
                .willReturn(false);

        requestService.sendSignupCode(email, CLIENT_ADDRESS);

        verify(emailVerificationService).sendCode(email, EmailVerificationPurpose.SIGNUP);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 발송 성공: 탈퇴하지 않은 이메일 계정은 발송한다")
    void sendPasswordResetCode_EmailUser_Success() {
        String requestedEmail = "MEMBER@example.com";
        String registeredEmail = "member@example.com";
        User user = createUser(registeredEmail, LoginType.EMAIL, UserStatus.INACTIVE);
        given(userRepository.findByEmail(requestedEmail)).willReturn(Optional.of(user));

        requestService.sendPasswordResetCode(requestedEmail, CLIENT_ADDRESS);

        verify(emailVerificationService)
                .sendCode(registeredEmail, EmailVerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 발송: 존재하지 않는 계정도 동일하게 성공 처리한다")
    void sendPasswordResetCode_UserNotFound_ReturnsWithoutSending() {
        String email = "unknown@example.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        requestService.sendPasswordResetCode(email, CLIENT_ADDRESS);

        verify(rateLimitService).checkSendAllowed(email, CLIENT_ADDRESS);
        verify(emailVerificationService, never())
                .sendCode(email, EmailVerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 발송: 탈퇴 계정도 동일하게 성공 처리한다")
    void sendPasswordResetCode_DeletedUser_ReturnsWithoutSending() {
        String email = "deleted@example.com";
        User user = createUser(email, LoginType.EMAIL, UserStatus.DELETED);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        requestService.sendPasswordResetCode(email, CLIENT_ADDRESS);

        verify(emailVerificationService, never())
                .sendCode(email, EmailVerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 발송: 소셜 계정도 동일하게 성공 처리한다")
    void sendPasswordResetCode_SocialUser_ReturnsWithoutSending() {
        String email = "social@example.com";
        User user = createUser(email, LoginType.KAKAO, UserStatus.ACTIVE);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        requestService.sendPasswordResetCode(email, CLIENT_ADDRESS);

        verify(emailVerificationService, never())
                .sendCode(email, EmailVerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("인증코드 검증: 요청 경로에 맞는 인증 목적을 전달한다")
    void verifyCode_DelegatesPurpose() {
        String email = "user@example.com";
        String code = "123456";

        requestService.verifySignupCode(email, code, CLIENT_ADDRESS);
        requestService.verifyPasswordResetCode(email, code, CLIENT_ADDRESS);

        verify(rateLimitService, org.mockito.Mockito.times(2)).checkVerifyAllowed(CLIENT_ADDRESS);
        verify(emailVerificationService)
                .verifyCode(email, code, EmailVerificationPurpose.SIGNUP);
        verify(emailVerificationService)
                .verifyCode(email, code, EmailVerificationPurpose.PASSWORD_RESET);
    }

    private User createUser(String email, LoginType loginType, UserStatus status) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .nickname("tester")
                .loginType(loginType)
                .friendCode("ABC123")
                .status(status)
                .build();
    }
}
