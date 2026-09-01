package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.dto.req.AuthReqDTO;
import com.my4cut.domain.auth.dto.res.AuthResDTO;
import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.repository.RefreshTokenRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.service.WorkspaceService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String VERIFICATION_TOKEN = "verification-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private AccountWithdrawalCleanupService accountWithdrawalCleanupService;

    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공: 회원가입 목적의 이메일 인증을 완료한 신규 사용자를 저장한다")
    void signup_NewUser_Success() {
        String email = "new@example.com";
        UserReqDTO.SignUpDTO request = new UserReqDTO.SignUpDTO(
                email, VERIFICATION_TOKEN, "Password1!", "tester"
        );
        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.SIGNUP, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(request);

        verify(userRepository).save(any(User.class));
        verify(workspaceService).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패: 비밀번호 재설정 인증 상태는 회원가입에 사용할 수 없다")
    void signup_PasswordResetVerification_Fail() {
        String email = "new@example.com";
        UserReqDTO.SignUpDTO request = new UserReqDTO.SignUpDTO(
                email, VERIFICATION_TOKEN, "Password1!", "tester"
        );
        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.SIGNUP, VERIFICATION_TOKEN
        )).willReturn(false);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_NOT_VERIFIED);

        verify(userRepository, never()).save(any(User.class));
        verify(workspaceService, never()).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패: 발송 후 동일 이메일 계정이 생성됐으면 최종 단계에서 다시 차단한다")
    void signup_DuplicateAfterVerification_Fail() {
        String email = "member@example.com";
        User existingUser = createEmailUser(email, passwordEncoder.encode("Password1!"), UserStatus.ACTIVE);
        UserReqDTO.SignUpDTO request = new UserReqDTO.SignUpDTO(
                email, VERIFICATION_TOKEN, "Password2!", "tester"
        );
        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.SIGNUP, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_DUPLICATE_EMAIL);

        verify(userRepository, never()).save(any(User.class));
        verify(workspaceService, never()).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("회원가입 성공: 기존 탈퇴 계정을 익명화하고 신규 사용자로 가입한다")
    void signup_DeletedUser_CreatesNewAccount() {
        String email = "deleted@example.com";
        User deletedUser = createEmailUser(email, passwordEncoder.encode("OldPassword1!"), UserStatus.DELETED);
        UserReqDTO.SignUpDTO request = new UserReqDTO.SignUpDTO(
                email, VERIFICATION_TOKEN, "NewPassword1!", "new-name"
        );
        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.SIGNUP, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(deletedUser));
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        authService.signup(request);

        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deletedUser.getEmail()).isNull();
        assertThat(deletedUser.getPassword()).isNull();
        assertThat(deletedUser.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(deletedUser.getFriendCode()).startsWith("DELETED_1_");
        verify(refreshTokenRepository).deleteByUser(deletedUser);
        verify(accountWithdrawalCleanupService).cleanup(deletedUser);
        verify(userRepository).flush();
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(newUser ->
                newUser != deletedUser
                        && email.equals(newUser.getEmail())
                        && newUser.getStatus() == UserStatus.ACTIVE
        ));
        verify(workspaceService).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("카카오 최초 가입 성공: 신규 사용자에게 기본 워크스페이스를 생성한다")
    void kakaoLogin_NewUser_CreatesDefaultWorkspace() {
        String accessToken = "kakao-access-token";
        String oauthId = "123456";
        AuthService spyAuthService = spy(authService);
        doReturn(new AuthResDTO.KakaoUserResDto(Long.valueOf(oauthId)))
                .when(spyAuthService).getKakaoUser(accessToken);
        given(userRepository.findByLoginTypeAndOauthId(LoginType.KAKAO, oauthId))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        spyAuthService.kakaoLogin(accessToken);

        verify(userRepository).save(any(User.class));
        verify(workspaceService).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("카카오 기존 사용자 로그인 성공: 기본 워크스페이스를 추가로 생성하지 않는다")
    void kakaoLogin_ExistingUser_DoesNotCreateDefaultWorkspace() {
        String accessToken = "kakao-access-token";
        String oauthId = "123456";
        User existingUser = User.builder()
                .loginType(LoginType.KAKAO)
                .oauthId(oauthId)
                .nickname("kakao-user")
                .friendCode("ABC123")
                .status(UserStatus.ACTIVE)
                .build();
        AuthService spyAuthService = spy(authService);
        doReturn(new AuthResDTO.KakaoUserResDto(Long.valueOf(oauthId)))
                .when(spyAuthService).getKakaoUser(accessToken);
        given(userRepository.findByLoginTypeAndOauthId(LoginType.KAKAO, oauthId))
                .willReturn(Optional.of(existingUser));

        spyAuthService.kakaoLogin(accessToken);

        verify(userRepository, never()).save(any(User.class));
        verify(workspaceService, never()).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("카카오 재가입 성공: 기존 탈퇴 계정을 익명화하고 신규 사용자로 가입한다")
    void kakaoLogin_DeletedUser_CreatesNewAccount() {
        String accessToken = "kakao-access-token";
        String oauthId = "123456";
        User deletedUser = User.builder()
                .loginType(LoginType.KAKAO)
                .oauthId(oauthId)
                .nickname("old-kakao-user")
                .friendCode("OLD123")
                .status(UserStatus.DELETED)
                .build();
        ReflectionTestUtils.setField(deletedUser, "id", 2L);
        AuthService spyAuthService = spy(authService);
        doReturn(new AuthResDTO.KakaoUserResDto(Long.valueOf(oauthId)))
                .when(spyAuthService).getKakaoUser(accessToken);
        given(userRepository.findByLoginTypeAndOauthId(LoginType.KAKAO, oauthId))
                .willReturn(Optional.of(deletedUser));
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        spyAuthService.kakaoLogin(accessToken);

        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deletedUser.getOauthId()).isNull();
        assertThat(deletedUser.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(deletedUser.getFriendCode()).startsWith("DELETED_2_");
        verify(refreshTokenRepository).deleteByUser(deletedUser);
        verify(accountWithdrawalCleanupService).cleanup(deletedUser);
        verify(userRepository).flush();
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(newUser ->
                newUser != deletedUser
                        && newUser.getLoginType() == LoginType.KAKAO
                        && oauthId.equals(newUser.getOauthId())
                        && newUser.getStatus() == UserStatus.ACTIVE
        ));
        verify(workspaceService).createDefaultWorkspace(any(User.class));
    }

    @Test
    @DisplayName("회원 탈퇴 성공: 인증정보와 활성 관계를 정리하고 계정을 익명화한다")
    void withdraw_Success_AnonymizesAccount() {
        User user = createEmailUser(
                "member@example.com",
                passwordEncoder.encode("Password1!"),
                UserStatus.ACTIVE
        );
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        authService.withdraw(user.getId());

        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(user.getFriendCode()).startsWith("DELETED_1_");
        verify(refreshTokenRepository).deleteByUser(user);
        verify(accountWithdrawalCleanupService).cleanup(user);
    }

    @Test
    @DisplayName("비밀번호 재설정 성공: 인증 완료 사용자의 비밀번호를 변경하고 리프레시 토큰을 삭제한다")
    void resetPassword_Success() {
        // Arrange
        String email = "test@example.com";
        String oldPassword = "OldPassw0rd!";
        String newPassword = "NewPassw0rd!";

        User user = createEmailUser(email, passwordEncoder.encode(oldPassword), UserStatus.ACTIVE);

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act
        authService.resetPassword(new AuthReqDTO.ResetPasswordReqDto(email, VERIFICATION_TOKEN, newPassword));

        // Assert
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 이메일 인증이 완료되지 않으면 예외가 발생한다")
    void resetPassword_Fail_NotVerified() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(
                email, VERIFICATION_TOKEN, "NewPassw0rd!"
        );

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(false);

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
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(
                email, VERIFICATION_TOKEN, "abcd1234"
        );

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);

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
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(
                email, VERIFICATION_TOKEN, "New Passw0rd!"
        );

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);

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

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(
                new AuthReqDTO.ResetPasswordReqDto(email, VERIFICATION_TOKEN, samePassword)
        ))
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

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(
                new AuthReqDTO.ResetPasswordReqDto(email, VERIFICATION_TOKEN, "NewPassw0rd!")
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패: 이메일 인증은 완료됐지만 사용자가 없으면 인증 정보 오류 예외가 발생한다")
    void resetPassword_Fail_UserNotFoundAfterVerified() {
        // Arrange
        String email = "test@example.com";
        AuthReqDTO.ResetPasswordReqDto request = new AuthReqDTO.ResetPasswordReqDto(
                email, VERIFICATION_TOKEN, "NewPassw0rd!"
        );

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);
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

        given(emailVerificationService.claimVerifiedForTransaction(
                email, EmailVerificationPurpose.PASSWORD_RESET, VERIFICATION_TOKEN
        )).willReturn(true);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(
                new AuthReqDTO.ResetPasswordReqDto(email, VERIFICATION_TOKEN, "NewPassw0rd!")
        ))
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
