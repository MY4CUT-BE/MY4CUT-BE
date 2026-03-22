package com.my4cut.domain.media.repository;

import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaObjectRepository extends JpaRepository<MediaObject, Long> {

    Optional<MediaObject> findByOwnerIdAndSha256AndFileSizeAndStatus(Long ownerId, String sha256, Long fileSize, MediaObjectStatus status);

    Optional<MediaObject> findByOwnerIdAndSha256AndFileSize(Long ownerId, String sha256, Long fileSize);
}
