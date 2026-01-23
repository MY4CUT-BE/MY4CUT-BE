package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MediaComment 엔티티에 대한 데이터 접근 기능을 제공하는 리포지토리 인터페이스.
 */
@Repository
public interface MediaCommentRepository extends JpaRepository<MediaComment, Long> {
    List<MediaComment> findAllByMediaFileIdOrderByCreatedAtDesc(Long mediaId);
}
