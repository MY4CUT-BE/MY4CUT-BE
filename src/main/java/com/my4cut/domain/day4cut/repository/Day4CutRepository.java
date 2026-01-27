package com.my4cut.domain.day4cut.repository;

import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 하루네컷 Repository 인터페이스.
 */
public interface Day4CutRepository extends JpaRepository<Day4Cut, Long> {

    Optional<Day4Cut> findByIdAndUser(Long id, User user);

    List<Day4Cut> findAllByUserOrderByDateDesc(User user);

    boolean existsByUserAndDate(User user, LocalDate date);
}
