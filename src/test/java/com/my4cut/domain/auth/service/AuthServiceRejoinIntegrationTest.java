package com.my4cut.domain.auth.service;

import com.my4cut.domain.auth.enums.EmailVerificationPurpose;
import com.my4cut.domain.auth.jwt.JwtProvider;
import com.my4cut.domain.auth.repository.RefreshTokenRepository;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.service.WorkspaceService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest
class AuthServiceRejoinIntegrationTest {

    private static final String VERIFICATION_TOKEN = "verification-token";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private UserFcmTokenRepository userFcmTokenRepository;

    @Test
    @DisplayName("탈퇴 계정의 이메일 UNIQUE 값을 해제한 뒤 같은 이메일로 신규 사용자를 저장한다")
    void signup_DeletedUser_PersistsNewAccountWithSameEmail() {
        String email = "rejoin@example.com";
        User deletedUser = User.builder()
                .email(email)
                .password("old-encoded-password")
                .nickname("old-user")
                .profileImageUrl("profile/old-user.png")
                .loginType(LoginType.EMAIL)
                .friendCode("OLD123")
                .status(UserStatus.DELETED)
                .build();
        entityManager.persist(deletedUser);
        entityManager.flush();
        entityManager.clear();

        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        AccountWithdrawalCleanupService cleanupService = new AccountWithdrawalCleanupService(
                friendRepository,
                friendRequestRepository,
                workspaceInvitationRepository,
                workspaceMemberRepository,
                userFcmTokenRepository
        );
        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                mock(JwtProvider.class),
                new BCryptPasswordEncoder(),
                emailVerificationService,
                workspaceService,
                cleanupService
        );
        given(emailVerificationService.claimVerifiedForTransaction(
                email,
                EmailVerificationPurpose.SIGNUP,
                VERIFICATION_TOKEN
        )).willReturn(true);
        UserReqDTO.SignUpDTO request = new UserReqDTO.SignUpDTO(
                email,
                VERIFICATION_TOKEN,
                "NewPassword1!",
                "new-user"
        );

        authService.signup(request);
        entityManager.flush();
        entityManager.clear();

        User anonymizedUser = userRepository.findById(deletedUser.getId()).orElseThrow();
        User newUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(anonymizedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(anonymizedUser.getEmail()).isNull();
        assertThat(anonymizedUser.getFriendCode()).startsWith("DELETED_");
        assertThat(newUser.getId()).isNotEqualTo(anonymizedUser.getId());
        assertThat(newUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(newUser.getNickname()).isEqualTo("new-user");
        verify(workspaceService).createDefaultWorkspace(org.mockito.ArgumentMatchers.argThat(savedUser ->
                savedUser.getId().equals(newUser.getId())
                        && email.equals(savedUser.getEmail())
        ));
    }
}
