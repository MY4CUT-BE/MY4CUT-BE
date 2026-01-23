package com.my4cut.domain.friend.repository;

import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
//아직 친구 처리되지않은 요청 -> 생성 시점 : 친구 요청 보낼 때
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsByFromUserAndToUserAndStatus(
            User fromUser,
            User toUser,
            FriendRequestStatus status
    );

    List<FriendRequest> findAllByToUserAndStatus(
            User toUser,
            FriendRequestStatus status
    );

    Optional<FriendRequest> findByIdAndToUser(Long id, User toUser);

    Optional<FriendRequest> findByIdAndFromUser(Long id, User fromUser);
}

