package com.my4cut.domain.notification.repository;

import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.enums.NotificationType;
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

    void deleteAllByUser(User user);
    List<Notification> findAllByIdInAndUser(List<Long> ids, User user);
    void deleteByUserAndTypeAndReferenceId(User user, NotificationType type, Long referenceId);

    /*
     * 친구 요청/스페이스 초대 알림은 원본 요청이 아직 PENDING일 때만 노출한다.
     * 수락/거절 후 DB에 남아 있는 과거 알림까지 조회 단계에서 제외해 알림창 잔류를 방지한다.
     */
    @Query(
            value = """
                    select n.*
                    from notifications n
                    where n.user_id = :userId
                      and (
                          n.type not in ('FRIEND_REQUEST', 'WORKSPACE_INVITE')
                          or (
                              n.type = 'FRIEND_REQUEST'
                              and exists (
                                  select 1
                                  from friend_requests fr
                                  where fr.id = n.reference_id
                                    and fr.status = 'PENDING'
                              )
                          )
                          or (
                              n.type = 'WORKSPACE_INVITE'
                              and exists (
                                  select 1
                                  from workspace_invitations wi
                                  where wi.id = n.reference_id
                                    and wi.status = 'PENDING'
                              )
                          )
                      )
                    order by n.created_at desc, n.id desc
                    """,
            countQuery = """
                    select count(*)
                    from notifications n
                    where n.user_id = :userId
                      and (
                          n.type not in ('FRIEND_REQUEST', 'WORKSPACE_INVITE')
                          or (
                              n.type = 'FRIEND_REQUEST'
                              and exists (
                                  select 1
                                  from friend_requests fr
                                  where fr.id = n.reference_id
                                    and fr.status = 'PENDING'
                              )
                          )
                          or (
                              n.type = 'WORKSPACE_INVITE'
                              and exists (
                                  select 1
                                  from workspace_invitations wi
                                  where wi.id = n.reference_id
                                    and wi.status = 'PENDING'
                              )
                          )
                      )
                    """,
            nativeQuery = true
    )
    Page<Notification> findVisibleByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );

    /*
     * 읽지 않은 상태도 실제 표시 가능한 알림 기준으로 계산한다.
     * 처리 완료된 요청 알림 때문에 알림 배지가 계속 켜지는 상황을 막는다.
     */
    @Query(
            value = """
                    select count(*)
                    from notifications n
                    where n.user_id = :userId
                      and n.is_read = false
                      and (
                          n.type not in ('FRIEND_REQUEST', 'WORKSPACE_INVITE')
                          or (
                              n.type = 'FRIEND_REQUEST'
                              and exists (
                                  select 1
                                  from friend_requests fr
                                  where fr.id = n.reference_id
                                    and fr.status = 'PENDING'
                              )
                          )
                          or (
                              n.type = 'WORKSPACE_INVITE'
                              and exists (
                                  select 1
                                  from workspace_invitations wi
                                  where wi.id = n.reference_id
                                    and wi.status = 'PENDING'
                              )
                          )
                      )
                    """,
            nativeQuery = true
    )
    long countVisibleUnreadByUserId(@Param("userId") Long userId);

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
