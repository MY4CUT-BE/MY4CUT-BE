package com.my4cut.domain.pose.repository;

import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.entity.PoseFavorite;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoseFavoriteRepository extends JpaRepository<PoseFavorite, Long> {
    boolean existsByUserAndPose(User user, Pose pose);
    boolean existsByUserIdAndPoseId(Long userId, Long poseId);

    @Query("""
            select favorite.pose.id
            from PoseFavorite favorite
            where favorite.user.id = :userId
              and favorite.pose.id in :poseIds
            """)
    List<Long> findFavoritePoseIds(
            @Param("userId") Long userId,
            @Param("poseIds") Collection<Long> poseIds
    );

    Optional<PoseFavorite> findByUserAndPose(User user, Pose pose);
    void deleteByUserAndPose(User user, Pose pose);
}
