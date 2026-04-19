package com.my4cut.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationCleanupService notificationCleanupService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${notification.cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${notification.cleanup.zone:Asia/Seoul}")
    private String cleanupZone;

    @Value("${notification.cleanup.batch-size:1000}")
    private int batchSize;

    /*
     * 기본 실행 시간은 매일 04:00 KST이다.
     * 알림 조회 트래픽이 적은 시간대에 실행하고, 운영에서는 NOTIFICATION_CLEANUP_CRON으로 조정할 수 있다.
     */
    @Scheduled(
            cron = "${notification.cleanup.cron:0 0 4 * * *}",
            zone = "${notification.cleanup.zone:Asia/Seoul}"
    )
    public void deleteOldNotifications() {
        // 이전 정리 작업이 아직 끝나지 않았으면 같은 인스턴스 안에서 중복 삭제 작업이 겹치지 않도록 건너뛴다.
        if (!running.compareAndSet(false, true)) {
            log.info("Notification cleanup skipped because previous job is still running.");
            return;
        }

        try {
            log.info("Notification cleanup started. retentionDays={}, zone={}, batchSize={}", retentionDays, cleanupZone, batchSize);
            int deletedCount = notificationCleanupService.deleteOlderThanRetentionDays(
                    retentionDays,
                    ZoneId.of(cleanupZone),
                    batchSize
            );
            log.info("Notification cleanup finished. deletedCount={}", deletedCount);
        } catch (Exception e) {
            log.warn("Notification cleanup failed.", e);
        } finally {
            running.set(false);
        }
    }
}
