package com.my4cut.domain.notification.service;

import com.my4cut.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    public int deleteOlderThan(LocalDateTime cutoff, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Notification cleanup batchSize must be greater than 0.");
        }

        int totalDeletedCount = 0;
        int deletedCount;

        do {
            // 운영 데이터 삭제는 한 번에 크게 지우지 않고 batchSize 단위로 끊어서 DB 부하를 제한한다.
            deletedCount = notificationRepository.deleteBatchByCreatedAtBefore(cutoff, batchSize);
            totalDeletedCount += deletedCount;
            log.info("Notification cleanup batch deleted {} rows. cutoff={}, batchSize={}", deletedCount, cutoff, batchSize);
        } while (deletedCount == batchSize);

        log.info("Notification cleanup deleted {} rows in total. cutoff={}", totalDeletedCount, cutoff);
        return totalDeletedCount;
    }

    public int deleteOlderThanRetentionDays(int retentionDays, ZoneId zoneId, int batchSize) {
        // 요구사항 기준은 Asia/Seoul이다. 서버나 DB timezone이 달라도 같은 삭제 경계를 쓰기 위해 zoneId로 cutoff를 계산한다.
        LocalDateTime cutoff = LocalDateTime.now(zoneId).minusDays(retentionDays);
        return deleteOlderThan(cutoff, batchSize);
    }
}
