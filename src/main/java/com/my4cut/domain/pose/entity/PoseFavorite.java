package com.my4cut.domain.pose.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 즐겨찾기한 포즈 정보를 저장하는 엔티티이다.
 * 사용자와 포즈 간의 즐겨찾기 관계를 관리한다.
 */
@Entity
@Table(name = "pose_favorites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PoseFavorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pose_id", nullable = false)
    private Pose pose;

    /**
     * Creates a PoseFavorite linking the given user to the given pose.
     *
     * @param user the user who favorites the pose; must not be null
     * @param pose the pose being favorited; must not be null
     */
    @Builder
    public PoseFavorite(User user, Pose pose) {
        this.user = user;
        this.pose = pose;
    }
}