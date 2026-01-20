package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * MediaFile 엔티티에 대한 데이터 접근 기능을 제공하는 리포지토리 인터페이스.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
}
