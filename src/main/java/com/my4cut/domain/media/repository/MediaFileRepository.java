package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MediaFile 엔티티에 대한 데이터 접근 기능을 제공하는 리포지토리 인터페이스.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    List<MediaFile> findAllByWorkspaceIdAndMediaType(Long workspaceId, MediaType mediaType, Sort sort);

    Page<MediaFile> findAllByUploader(User uploader, Pageable pageable);

    boolean existsByWorkspaceIdAndIsFinalTrue(Long workspaceId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update MediaFile mediaFile
            set mediaFile.isFinal = false
            where mediaFile.workspace.id = :workspaceId
              and mediaFile.mediaType = :mediaType
              and mediaFile.id <> :photoId
            """)
    void clearFinalPhotosExcept(
            @Param("workspaceId") Long workspaceId,
            @Param("mediaType") MediaType mediaType,
            @Param("photoId") Long photoId
    );

    long countByMediaObjectId(Long mediaObjectId);
}
