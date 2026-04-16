package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.dto.req.AuthReqDTO;
import com.my4cut.domain.auth.repository.RefreshTokenRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("비밀번호 재설정 성공: 인증 완료 사용자의 비밀번호를 변경하고 리프레시 토큰을 삭제한다")
    void resetPassword_Success() {
        // Arrange
        String email = "test@example.com";
        String oldPassword = "OldPassw0rd!";
        String newPassword = "NewPassw0rd!";

        User user = createEmailUser(email, passwordEncoder.encode(oldPassword), UserStatus.ACTIVE);

        given(emailVerificationService.isVerified(email)).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act
        authService.resetPassword(new AuthReqDTO.ResetPasswordReqDto(email, newPassword));

        // Assert
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
        verify(refreshTokenRepository).deleteByUser(user);
        verify(emailVerificationService).clearVerifiedAfterCommit(email);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 이메일 인증이 완료되지 않으면 예외가 발생한다")
    void resetPassword_Fail_NotVerified() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(email, "NewPassw0rd!");

        given(emailVerificationService.isVerified(email)).willReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_NOT_VERIFIED);

        verify(userRepository, never()).findByEmail(email);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 비밀번호 정책을 만족하지 않으면 예외가 발생한다")
    void resetPassword_Fail_PolicyViolation() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(email, "abcd1234");

        given(emailVerificationService.isVerified(email)).willReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);

        verify(userRepository, never()).findByEmail(email);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 공백이 포함된 비밀번호는 정책 위반 예외가 발생한다")
    void resetPassword_Fail_WhitespaceInPassword() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(email, "New Passw0rd!");

        given(emailVerificationService.isVerified(email)).willReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_PASSWORD_POLICY_VIOLATION);

        verify(userRepository, never()).findByEmail(email);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 기존 비밀번호와 동일하면 예외가 발생한다")
    void resetPassword_Fail_SamePassword() {
        // Arrange
        String email = "test@example.com";
        String samePassword = "SamePassw0rd!";
        User user = createEmailUser(email, passwordEncoder.encode(samePassword), UserStatus.ACTIVE);

        given(emailVerificationService.isVerified(email)).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(new AuthReqDTO.ResetPasswordReqDto(email, samePassword)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_PASSWORD_SAME_AS_OLD);

        verify(refreshTokenRepository, never()).deleteByUser(user);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 탈퇴한 사용자는 비밀번호를 변경할 수 없다")
    void resetPassword_Fail_DeletedUser() {
        // Arrange
        String email = "test@example.com";
        User user = createEmailUser(email, passwordEncoder.encode("OldPassw0rd!"), UserStatus.DELETED);

        given(emailVerificationService.isVerified(email)).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(new AuthReqDTO.ResetPasswordReqDto(email, "NewPassw0rd!")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_DELETED);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 이메일 인증은 완료됐지만 사용자가 없으면 인증 정보 오류 예외가 발생한다")
    void resetPassword_Fail_UserNotFoundAfterVerified() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(email, "NewPassw0rd!");

        given(emailVerificationService.isVerified(email)).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_CREDENTIALS);

        verify(refreshTokenRepository, never()).deleteByUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 소셜 로그인 계정은 비밀번호 재설정을 사용할 수 없다")
    void resetPassword_Fail_SocialUser() {
        // Arrange
        String email = "test@example.com";
        User user = createEmailUser(email, passwordEncoder.encode("OldPassw0rd!"), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "loginType", LoginType.KAKAO);

        given(emailVerificationService.isVerified(email)).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(new AuthReqDTO.ResetPasswordReqDto(email, "NewPassw0rd!")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_PASSWORD_RESET_NOT_ALLOWED);
    }

    private User createEmailUser(String email, String encodedPassword, UserStatus status) {
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname("tester")
                .loginType(LoginType.EMAIL)
                .friendCode("ABC123")
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
