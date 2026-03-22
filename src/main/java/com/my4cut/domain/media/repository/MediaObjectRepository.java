package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaObjectRepository extends JpaRepository<MediaObject, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaObject> findByOwnerIdAndSha256AndFileSizeAndStatus(Long ownerId, String sha256, Long fileSize, MediaObjectStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaObject> findByOwnerIdAndSha256AndFileSize(Long ownerId, String sha256, Long fileSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaObject> findById(Long id);

    List<MediaObject> findTop100ByStatusOrderByIdAsc(MediaObjectStatus status);
}
