package com.my4cut.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaObjectCleanupScheduler {

    private static final int BATCH_SIZE = 100;

    private final MediaObjectCleanupService mediaObjectCleanupService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${media.cleanup.pending-delete-fixed-delay-ms:300000}")
    public void retryPendingDeletes() {
        // 같은 인스턴스 안에서는 정리 작업이 겹치지 않게 막는다.
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            List<Long> pendingDeleteIds = mediaObjectCleanupService.findPendingDeleteIds(BATCH_SIZE);
            for (Long pendingDeleteId : pendingDeleteIds) {
                try {
                    mediaObjectCleanupService.retryPendingDelete(pendingDeleteId);
                } catch (Exception e) {
                    log.warn("Pending delete retry job failed for mediaObjectId={}", pendingDeleteId, e);
                }
            }
        } finally {
            running.set(false);
        }
    }
}
