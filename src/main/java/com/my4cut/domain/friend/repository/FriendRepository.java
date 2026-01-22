package com.my4cut.domain.friend.repository;

import com.my4cut.domain.friend.entity.Friend;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
//이미 친구 관계 -> 생성 시점 : 친구 요청이 수락됐을 때
public interface FriendRepository extends JpaRepository<Friend, Long> {

    boolean existsByUserAndFriendUser(User fromUser, User toUser);

    Optional<Friend> findByUserAndFriendUser(User fromUser, User toUser);

    List<Friend> findAllByUser(User user);
}
