package com.my4cut.domain.friend.service;

import com.my4cut.domain.friend.dto.res.FriendResDto;
import com.my4cut.domain.friend.entity.Friend;
import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.image.service.ProfileImageUrlService;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendRepository friendRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private NotificationService notificationService;
    @Mock private ProfileImageUrlService profileImageUrlService;

    @InjectMocks
    private FriendService friendService;

    @Test
    void getMyFriends_ReturnsHttpProfileImageUrls() {
        Long userId = 1L;
        User user = createUser(userId, "me", null);
        User friendUser = createUser(2L, "friend", "profile/friend.png");
        Friend friend = Friend.builder()
                .user(user)
                .friendUser(friendUser)
                .isFavorite(false)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(friendRepository.findAllByUser(user)).willReturn(List.of(friend));
        given(profileImageUrlService.toResponseUrl("profile/friend.png"))
                .willReturn("http://localhost:8080/images/profile/friend.png");

        List<FriendResDto> result = friendService.getMyFriends(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProfileImageUrl())
                .isEqualTo("http://localhost:8080/images/profile/friend.png");
    }

    @Test
    void searchUserByFriendCode_ReturnsHttpProfileImageUrl() {
        Long userId = 1L;
        String friendCode = "ABC123";
        User user = createUser(userId, "me", null);
        User target = createUser(2L, "target", "profile/target.png");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.findByFriendCode(friendCode)).willReturn(Optional.of(target));
        given(friendRepository.existsByUserAndFriendUser(user, target)).willReturn(false);
        given(friendRequestRepository.existsByFromUserAndToUserAndStatus(
                user,
                target,
                FriendRequestStatus.PENDING))
                .willReturn(false);
        given(friendRequestRepository.existsByFromUserAndToUserAndStatus(
                target,
                user,
                FriendRequestStatus.PENDING))
                .willReturn(false);
        given(profileImageUrlService.toResponseUrl("profile/target.png"))
                .willReturn("http://localhost:8080/images/profile/target.png");

        FriendResDto.SearchUserResDto result = friendService.searchUserByFriendCode(userId, friendCode);

        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.nickname()).isEqualTo("target");
        assertThat(result.profileImageUrl()).isEqualTo("http://localhost:8080/images/profile/target.png");
        assertThat(result.alreadyFriend()).isFalse();
        assertThat(result.outgoingRequest()).isFalse();
        assertThat(result.incomingRequest()).isFalse();
    }

    @Test
    void cancelSentRequest_DeletesFriendRequestNotification() {
        Long userId = 1L;
        Long requestId = 10L;
        User fromUser = createUser(userId, "me", null);
        User toUser = createUser(2L, "other", null);
        FriendRequest request = createPendingRequest(requestId, fromUser, toUser);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        friendService.cancelSentRequest(userId, requestId);

        verify(notificationService).deleteFriendRequestNotification(toUser, requestId);
    }

    @Test
    void acceptFriendRequest_DeletesFriendRequestNotification() {
        Long userId = 2L;
        Long requestId = 10L;
        User fromUser = createUser(1L, "sender", null);
        User toUser = createUser(userId, "me", null);
        FriendRequest request = createPendingRequest(requestId, fromUser, toUser);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        friendService.acceptFriendRequest(userId, requestId);

        verify(notificationService).deleteFriendRequestNotification(toUser, requestId);
    }

    @Test
    void rejectFriendRequest_DeletesFriendRequestNotification() {
        Long userId = 2L;
        Long requestId = 10L;
        User fromUser = createUser(1L, "sender", null);
        User toUser = createUser(userId, "me", null);
        FriendRequest request = createPendingRequest(requestId, fromUser, toUser);

        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        friendService.rejectFriendRequest(userId, requestId);

        verify(notificationService).deleteFriendRequestNotification(toUser, requestId);
    }

    private User createUser(Long id, String nickname, String profileImageUrl) {
        User user = User.builder()
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private FriendRequest createPendingRequest(Long id, User fromUser, User toUser) {
        FriendRequest request = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
