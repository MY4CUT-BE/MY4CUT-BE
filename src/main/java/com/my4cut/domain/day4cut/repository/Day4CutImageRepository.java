package com.my4cut.domain.day4cut.repository;

import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.entity.Day4CutImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 하루네컷 이미지 Repository 인터페이스.
 */
public interface Day4CutImageRepository extends JpaRepository<Day4CutImage, Long> {

    List<Day4CutImage> findAllByDay4Cut(Day4Cut day4Cut);

    void deleteAllByDay4Cut(Day4Cut day4Cut);
}
