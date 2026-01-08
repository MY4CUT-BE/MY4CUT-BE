package com.my4cut.domain.friend.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 관계 정보를 저장하는 엔티티이다.
 * 사용자 간의 친구 관계와 즐겨찾기 여부를 관리한다.
 */
@Entity
@Table(name = "friends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friend extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    private User friendUser;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite;

    @Builder
    public Friend(User user, User friendUser, Boolean isFavorite) {
        this.user = user;
        this.friendUser = friendUser;
        this.isFavorite = isFavorite;
    }
}
