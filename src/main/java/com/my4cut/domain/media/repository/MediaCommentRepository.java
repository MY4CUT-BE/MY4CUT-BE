package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MediaComment 엔티티에 대한 데이터 접근 기능을 제공하는 리포지토리 인터페이스.
 */
@Repository
public interface MediaCommentRepository extends JpaRepository<MediaComment, Long> {
    interface MediaCommentCount {
        Long getMediaId();

        Long getCommentCount();
    }

    List<MediaComment> findAllByMediaFileIdOrderByCreatedAtDesc(Long mediaId);

    @Query("""
            select comment.mediaFile.id as mediaId, count(comment.id) as commentCount
            from MediaComment comment
            where comment.mediaFile.id in :mediaFileIds
            group by comment.mediaFile.id
            """)
    List<MediaCommentCount> countByMediaFileIds(@Param("mediaFileIds") List<Long> mediaFileIds);

    Optional<MediaComment> findTopByMediaFileWorkspaceIdOrderByCreatedAtDesc(
            Long workspaceId
    );
}
