package com.my4cut.domain.friend.service;

import com.my4cut.domain.friend.dto.res.FriendRequestResDto;
import com.my4cut.domain.friend.dto.res.FriendResDto;
import com.my4cut.domain.friend.entity.Friend;
import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.friend.repository.FriendRepository;
import com.my4cut.domain.friend.repository.FriendRequestRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;

    //친구 요청 보내기
    @Transactional
    public FriendRequestResDto.SendRequestResDto sendFriendRequest(Long userId, String targetFriendCode) {
        //친구 요청을 보내는 사람 - userId로 확인.
        User fromUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        //친구 요청을 받는 사람 - targetFriendCode(친구코드)로 확인
        User toUser = userRepository.findByFriendCode(targetFriendCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 이미 친구인지 확인
        if (friendRepository.existsByUserAndFriendUser(fromUser, toUser)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 이미 보낸 대기 중 요청이 있는지 확인
        if (friendRequestRepository.existsByFromUserAndToUserAndStatus(
                fromUser, toUser, FriendRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        FriendRequest request = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .status(FriendRequestStatus.PENDING)
                .build();

        FriendRequest savedRequest = friendRequestRepository.save(request);

        return FriendRequestResDto.SendRequestResDto.of(savedRequest);
    }

    //받은 요청 조회
    @Transactional(readOnly = true)
    public List<FriendRequestResDto.ReceivedRequestResDto> getReceivedRequests(Long userId) {
        //userId로 user 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return friendRequestRepository
                .findAllByToUserAndStatus(user, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResDto.ReceivedRequestResDto::of)
                .toList();
    }

    //보낸 요청 취소
    @Transactional
    public void cancelSentRequest(Long userId, Long requestId) {
        // 요청 조회
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 본인이 보낸 요청인지 확인
        if (!request.getFromUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // PENDING 상태인지 확인
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        friendRequestRepository.delete(request);
    }

    //요청 수락
    @Transactional
    public FriendRequestResDto.AcceptRequestResDto acceptFriendRequest(Long userId, Long requestId) {
        // 요청 조회
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 본인에게 온 요청인지 확인
        if (!request.getToUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // PENDING 상태인지 확인
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 요청 상태 변경
        request.accept();

        // 양방향 친구 관계 생성
        Friend friend1 = Friend.builder()
                .user(request.getFromUser())
                .friendUser(request.getToUser())
                .isFavorite(false)
                .build();

        Friend friend2 = Friend.builder()
                .user(request.getToUser())
                .friendUser(request.getFromUser())
                .isFavorite(false)
                .build();

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        return FriendRequestResDto.AcceptRequestResDto.of(request);
    }

    // 요청 거절
    @Transactional
    public FriendRequestResDto.RejectRequestResDto rejectFriendRequest(
            Long userId,
            Long requestId
    ) {
        // 요청 조회
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 본인에게 온 요청인지 확인
        if (!request.getToUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // PENDING 상태인지 확인
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 상태 변경
        request.reject();

        // 응답 DTO 반환
        return FriendRequestResDto.RejectRequestResDto.of(request);
    }

    // 친구 즐겨찾기
    @Transactional
    public FriendResDto.FavoriteFriendResDto favoriteFriend(Long userId, Long friendId) {
        //유저없음
        User fromUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        //친구유저없음
        User toUser = userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        //친구관계없음
        Friend friend = friendRepository.findByUserAndFriendUser(fromUser, toUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        friend.markFavorite();
        return FriendResDto.FavoriteFriendResDto.of(true);
    }

    // 친구 즐겨찾기 해제
    @Transactional
    public FriendResDto.FavoriteFriendResDto unfavoriteFriend(Long userId, Long friendId) {
        //유저없음
        User fromUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        //친구유저없음
        User toUser = userRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        //친구관계없음
        Friend friend = friendRepository.findByUserAndFriendUser(fromUser, toUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        friend.unmarkFavorite();
        return FriendResDto.FavoriteFriendResDto.of(false);
    }
}
