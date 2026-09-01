package com.my4cut.domain.auth.service;

import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.repository.WorkspaceInvitationRepository;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴 사용자가 더 이상 활성 사용자 관계에 노출되지 않도록 가벼운 관계 데이터만 즉시 정리한다.
 * 사진, 댓글, 워크스페이스 등 과거 콘텐츠는 후속 삭제 작업을 위해 유지한다.
 */
@Service
@RequiredArgsConstructor
public class AccountWithdrawalCleanupService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void cleanup(User user) {
        friendRepository.deleteAllInvolvingUser(user);
        friendRequestRepository.deleteAllPendingInvolvingUser(user, FriendRequestStatus.PENDING);
        workspaceInvitationRepository.deleteAllPendingInvolvingUser(user, InvitationStatus.PENDING);
        workspaceMemberRepository.deleteAllByUser(user);
        userFcmTokenRepository.deleteAllByUser(user);
    }
}
