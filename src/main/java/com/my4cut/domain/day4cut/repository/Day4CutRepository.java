package com.my4cut.domain.day4cut.repository;

import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 하루네컷 Repository 인터페이스.
 */
public interface Day4CutRepository extends JpaRepository<Day4Cut, Long> {

    Optional<Day4Cut> findByIdAndUser(Long id, User user);

    @Query("SELECT d FROM Day4Cut d " +
            "LEFT JOIN FETCH d.images i " +
            "LEFT JOIN FETCH i.mediaFile " +
            "WHERE d.user = :user AND d.date = :date")
    Optional<Day4Cut> findByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    List<Day4Cut> findAllByUserOrderByDateDesc(User user);

    boolean existsByUserAndDate(User user, LocalDate date);

    long countByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 연도와 월에 해당하는 하루네컷을 이미지(썸네일)와 함께 조회
     */
    @Query("SELECT d FROM Day4Cut d " +
            "LEFT JOIN FETCH d.images i " +
            "LEFT JOIN FETCH i.mediaFile " +
            "WHERE d.user = :user " +
            "AND YEAR(d.date) = :year " +
            "AND MONTH(d.date) = :month " +
            "ORDER BY d.date")
    List<Day4Cut> findAllByUserAndYearMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

}
