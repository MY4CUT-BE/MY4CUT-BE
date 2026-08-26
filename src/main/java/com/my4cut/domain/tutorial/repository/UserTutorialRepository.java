package com.my4cut.domain.tutorial.repository;

import com.my4cut.domain.tutorial.entity.UserTutorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTutorialRepository extends JpaRepository<UserTutorial, Long> {

    List<UserTutorial> findAllByUserId(Long userId);
}
