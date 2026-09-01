package com.my4cut.domain.auth.service;

import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalCleanupServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private WorkspaceInvitationRepository workspaceInvitationRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserFcmTokenRepository userFcmTokenRepository;

    @InjectMocks
    private AccountWithdrawalCleanupService cleanupService;

    @Test
    @DisplayName("회원 탈퇴 시 친구, 요청, 초대, 멤버십, FCM 토큰을 정리한다")
    void cleanup_DeletesActiveRelationships() {
        User user = User.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("member")
                .loginType(LoginType.EMAIL)
                .friendCode("ABC123")
                .status(UserStatus.ACTIVE)
                .build();

        cleanupService.cleanup(user);

        verify(friendRepository).deleteAllInvolvingUser(user);
        verify(friendRequestRepository)
                .deleteAllPendingInvolvingUser(user, FriendRequestStatus.PENDING);
        verify(workspaceInvitationRepository)
                .deleteAllPendingInvolvingUser(user, InvitationStatus.PENDING);
        verify(workspaceMemberRepository).deleteAllByUser(user);
        verify(userFcmTokenRepository).deleteAllByUser(user);
    }
}
