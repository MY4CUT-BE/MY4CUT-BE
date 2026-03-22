package com.my4cut.domain.media.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.repository.MediaObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaObjectCleanupService {

    private final MediaObjectRepository mediaObjectRepository;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public List<Long> findPendingDeleteIds(int limit) {
        return mediaObjectRepository.findTop100ByStatusOrderByIdAsc(MediaObjectStatus.PENDING_DELETE).stream()
                .limit(limit)
                .map(MediaObject::getId)
                .toList();
    }

    @Transactional
    public void retryPendingDelete(Long mediaObjectId) {
        // 삭제 재시도도 동일 row 를 lock 으로 잡아 업로드/삭제와 충돌하지 않게 한다.
        MediaObject mediaObject = mediaObjectRepository.findById(mediaObjectId)
                .orElse(null);

        if (mediaObject == null || mediaObject.getStatus() != MediaObjectStatus.PENDING_DELETE) {
            return;
        }

        boolean deleted = imageStorageService.deleteIfExists(mediaObject.getFileKey());
        if (!deleted) {
            log.warn("Pending delete retry failed. mediaObjectId={}, fileKey={}", mediaObject.getId(), mediaObject.getFileKey());
            return;
        }

        mediaObject.markDeleted();
        mediaObjectRepository.save(mediaObject);
    }
}
