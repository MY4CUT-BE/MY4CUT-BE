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

        // 업로드와 마지막 참조 삭제가 엇갈릴 수 있어 재사용 후보는 row lock으로 확인한다.
        MediaObject activeMediaObject = mediaObjectRepository
                .findByOwnerIdAndSha256AndFileSizeAndStatus(
                        user.getId(),
                        hashedMedia.sha256(),
                        hashedMedia.fileSize(),
                        MediaObjectStatus.ACTIVE
                )
                .orElse(null);

        if (activeMediaObject != null) {
            // business-facing MediaFile 은 새로 만들고 실제 파일만 재사용한다.
            return mediaFileRepository.save(createBusinessMediaFile(user, mediaType, activeMediaObject));
        }

        String uploadedFileKey = imageStorageService.upload(
                hashedMedia.bytes(),
                hashedMedia.originalFilename(),
                hashedMedia.contentType(),
                directory
        );

        try {
            MediaObject mediaObject = mediaObjectRepository
                    .findByOwnerIdAndSha256AndFileSize(user.getId(), hashedMedia.sha256(), hashedMedia.fileSize())
                    .map(existing -> {
                        // 경합 상황에서 기존 row 가 있으면 해당 row 를 재활성화해 사용한다.
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
        } catch (RuntimeException e) {
            cleanupUploadedFile(uploadedFileKey);
            throw e;
        }
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

        // 같은 media_object 에 대한 업로드/삭제를 직렬화해 마지막 참조 판정을 안정적으로 만든다.
        MediaObject lockedMediaObject = mediaObjectRepository.findById(mediaObject.getId())
                .orElseThrow(() -> new IllegalStateException("MediaObject not found: " + mediaObject.getId()));

        long referenceCount = mediaFileRepository.countByMediaObjectId(lockedMediaObject.getId());
        if (referenceCount > 0) {
            // 아직 다른 MediaFile 이 참조 중이면 실제 파일은 삭제하지 않는다.
            return;
        }

        // DB 에 먼저 삭제 대기 상태를 남기고 그 다음 실제 파일 삭제를 시도한다.
        lockedMediaObject.markPendingDelete();
        mediaObjectRepository.saveAndFlush(lockedMediaObject);

        boolean deleted = imageStorageService.deleteIfExists(lockedMediaObject.getFileKey());
        if (!deleted) {
            // 스토리지 삭제 실패 시 PENDING_DELETE 상태를 유지해 후속 정리가 가능하게 둔다.
            log.warn("Physical media delete failed. mediaObjectId={}, fileKey={}", lockedMediaObject.getId(), lockedMediaObject.getFileKey());
            return;
        }

        lockedMediaObject.markDeleted();
        mediaObjectRepository.save(lockedMediaObject);
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
            // 동시에 같은 파일이 업로드된 경우 방금 올린 파일은 지우고 기존 row 를 다시 읽는다.
            cleanupUploadedFile(uploadedFileKey);
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

    private void cleanupUploadedFile(String uploadedFileKey) {
        boolean deleted = imageStorageService.deleteIfExists(uploadedFileKey);
        if (!deleted) {
            log.warn("Failed to cleanup uploaded file after DB rollback path. fileKey={}", uploadedFileKey);
        }
    }
}
