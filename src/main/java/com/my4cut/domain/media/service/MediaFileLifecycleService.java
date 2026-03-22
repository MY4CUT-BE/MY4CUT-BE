package com.my4cut.domain.media.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.media.repository.MediaObjectRepository;
import com.my4cut.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaFileLifecycleService {

    private final MediaHashService mediaHashService;
    private final MediaObjectRepository mediaObjectRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ImageStorageService imageStorageService;

    @Transactional
    public MediaFile createMediaFile(User user, MultipartFile file, String directory, MediaType mediaType) {
        MediaHashService.HashedMedia hashedMedia = mediaHashService.hash(file);

        // 같은 사용자 범위에서만 dedup 한다.
        MediaObject activeMediaObject = mediaObjectRepository
                .findByOwnerIdAndSha256AndFileSizeAndStatus(
                        user.getId(),
                        hashedMedia.sha256(),
                        hashedMedia.fileSize(),
                        MediaObjectStatus.ACTIVE
                )
                .orElse(null);

        if (activeMediaObject != null) {
            // business-facing MediaFile 은 새로 만들고, 실제 저장 객체만 재사용한다.
            return mediaFileRepository.save(createBusinessMediaFile(user, mediaType, activeMediaObject));
        }

        String uploadedFileKey = imageStorageService.upload(
                hashedMedia.bytes(),
                hashedMedia.originalFilename(),
                hashedMedia.contentType(),
                directory
        );

        MediaObject mediaObject = mediaObjectRepository
                .findByOwnerIdAndSha256AndFileSize(user.getId(), hashedMedia.sha256(), hashedMedia.fileSize())
                .map(existing -> {
                    // 경합 상황에서 이미 만들어진 객체가 있으면 그 객체를 재활성화해서 사용한다.
                    existing.activate(
                            hashedMedia.sha256(),
                            uploadedFileKey,
                            hashedMedia.fileSize(),
                            hashedMedia.contentType()
                    );
                    return existing;
                })
                .orElseGet(() -> MediaObject.builder()
                        .owner(user)
                        .sha256(hashedMedia.sha256())
                        .fileKey(uploadedFileKey)
                        .fileSize(hashedMedia.fileSize())
                        .contentType(hashedMedia.contentType())
                        .status(MediaObjectStatus.ACTIVE)
                        .build());

        MediaObject savedMediaObject = saveMediaObjectWithRaceHandling(
                user.getId(),
                hashedMedia.sha256(),
                hashedMedia.fileSize(),
                uploadedFileKey,
                mediaObject
        );

        return mediaFileRepository.save(createBusinessMediaFile(user, mediaType, savedMediaObject));
    }

    @Transactional
    public void deleteMediaFile(MediaFile mediaFile) {
        MediaObject mediaObject = mediaFile.getMediaObject();
        String fileKey = mediaObject != null ? mediaObject.getFileKey() : mediaFile.getFileUrl();

        // 참조 수를 정확히 계산하기 위해 business row 삭제를 먼저 flush 한다.
        mediaFileRepository.delete(mediaFile);
        mediaFileRepository.flush();

        if (mediaObject == null) {
            imageStorageService.deleteIfExists(fileKey);
            return;
        }

        long referenceCount = mediaFileRepository.countByMediaObjectId(mediaObject.getId());
        if (referenceCount > 0) {
            // 아직 다른 MediaFile 이 참조 중이면 실제 파일은 삭제하지 않는다.
            return;
        }

        // DB 에서 더 이상 참조가 없다는 상태를 먼저 남기고, 그 다음 실제 파일을 지운다.
        mediaObject.markPendingDelete();
        mediaObjectRepository.saveAndFlush(mediaObject);

        boolean deleted = imageStorageService.deleteIfExists(mediaObject.getFileKey());
        if (!deleted) {
            // 스토리지 삭제 실패 시 PENDING_DELETE 상태를 유지해 후속 정리가 가능하게 둔다.
            log.warn("Physical media delete failed. mediaObjectId={}, fileKey={}", mediaObject.getId(), mediaObject.getFileKey());
            return;
        }

        mediaObject.markDeleted();
        mediaObjectRepository.save(mediaObject);
    }

    private MediaObject saveMediaObjectWithRaceHandling(
            Long ownerId,
            String sha256,
            Long fileSize,
            String uploadedFileKey,
            MediaObject mediaObject
    ) {
        try {
            return mediaObjectRepository.saveAndFlush(mediaObject);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 파일이 업로드된 경우 방금 올린 파일은 지우고 기존 객체를 다시 읽는다.
            imageStorageService.deleteIfExists(uploadedFileKey);
            return mediaObjectRepository
                    .findByOwnerIdAndSha256AndFileSizeAndStatus(ownerId, sha256, fileSize, MediaObjectStatus.ACTIVE)
                    .orElseThrow(() -> e);
        }
    }

    private MediaFile createBusinessMediaFile(User user, MediaType mediaType, MediaObject mediaObject) {
        // 기존 DTO/조회 로직 호환을 위해 fileUrl 에는 계속 fileKey 를 넣는다.
        return MediaFile.builder()
                .uploader(user)
                .mediaType(mediaType)
                .fileUrl(mediaObject.getFileKey())
                .mediaObject(mediaObject)
                .isFinal(false)
                .build();
    }
}
