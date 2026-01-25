package com.my4cut.domain.pose.repository;

import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.entity.PoseFavorite;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PoseFavoriteRepository extends JpaRepository<PoseFavorite, Long> {
    boolean existsByUserAndPose(User user, Pose pose);
    Optional<PoseFavorite> findByUserAndPose(User user, Pose pose);
    void deleteByUserAndPose(User user, Pose pose);
}
