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

    List<Day4Cut> findAllByUserOrderByDateDesc(User user);

    boolean existsByUserAndDate(User user, LocalDate date);

    /**
     * 네컷이 있는 캘린더를 가져오기 위한 쿼리문
     * 특정 연도와 월에 하루네컷이 존재하는 날짜 조회
     * user : 조회할 사용자
     * year : 조회할 연도 (예: 2026)
     * month : 조회할 월 (예: 1~12)
     */
    @Query("SELECT DISTINCT DAY(d.date) FROM Day4Cut d " +
            "WHERE d.user = :user " +
            "AND YEAR(d.date) = :year " +
            "AND MONTH(d.date) = :month " +
            "ORDER BY DAY(d.date)")

    /**
     * 하루네컷이 존재하는 날짜 리스트 (예: [1, 5, 15, 23])
     */
    List<Integer> findDaysByUserAndYearMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );
}
