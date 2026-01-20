package com.my4cut.domain.friend.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 요청 정보를 저장하는 엔티티이다.
 * 요청을 보낸 사용자와 받는 사용자, 요청 상태를 관리한다.
 */
@Entity
@Table(name = "friend_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    /**
     * Create a FriendRequest with the specified sender, recipient, and status.
     *
     * @param fromUser the user who sends the friend request
     * @param toUser   the user who receives the friend request
     * @param status   the initial status of the friend request
     */
    @Builder
    public FriendRequest(User fromUser, User toUser, FriendRequestStatus status) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = status;
    }
}