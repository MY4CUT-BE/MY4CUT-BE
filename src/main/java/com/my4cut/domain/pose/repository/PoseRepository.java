package com.my4cut.domain.pose.repository;

import com.my4cut.domain.pose.entity.Pose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoseRepository extends JpaRepository<Pose, Long> {
    List<Pose> findAllByPeopleCount(Integer peopleCount);
}
