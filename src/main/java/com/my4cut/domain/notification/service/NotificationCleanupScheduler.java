package com.my4cut.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private static final String LOCK_KEY = "notification:cleanup:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final NotificationCleanupService notificationCleanupService;
    private final StringRedisTemplate redisTemplate;

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
        // 이전 정리 작업이 아직 끝나지 않았으면 전체 인스턴스에서 중복 삭제 작업이 겹치지 않도록 건너뛴다.
        String lockToken = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, lockToken, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Notification cleanup skipped because another instance is running.");
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
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(LOCK_KEY), lockToken);
        }
    }
}
