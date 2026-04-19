package com.my4cut.domain.notification.repository;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    void deleteAllByUser(User user);
    Page<Notification> findByUserAndIsReadFalse(User user, Pageable pageable);
    boolean existsByUserAndIsReadFalse(User user);
    List<Notification> findAllByIdInAndUser(List<Long> ids, User user);

    /*
     * 30일 초과 알림을 한 번에 모두 삭제하면 운영 DB에서 긴 트랜잭션과 row lock 부담이 커질 수 있다.
     * 그래서 created_at 기준 삭제 대상을 limit 개수만큼만 잘라서 삭제하고,
     * 서비스에서 삭제 건수가 batchSize보다 작아질 때까지 반복 호출한다.
     *
     * MySQL은 "delete from t where id in (select id from t ... limit ...)" 형태를 제한할 수 있어,
     * 내부 select를 한 번 더 감싼 derived table(old_notifications) 형태로 작성했다.
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    delete from notifications
                    where id in (
                        select id
                        from (
                            select id
                            from notifications
                            where created_at < :cutoff
                            order by created_at, id
                            limit :limit
                        ) old_notifications
                    )
                    """,
            nativeQuery = true
    )
    int deleteBatchByCreatedAtBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );
}
