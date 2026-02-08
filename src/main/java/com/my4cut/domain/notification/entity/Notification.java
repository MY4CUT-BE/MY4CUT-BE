package com.my4cut.domain.notification.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.notification.enums.NotificationType;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 정보를 저장하는 엔티티이다.
 * 친구 요청, 워크스페이스 초대 등 다양한 알림을 사용자에게 전달하기 위한 정보를 관리한다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림을 발생시킨 사용자 (친구 요청 보낸 사람, 댓글 단 사람)
    @Column(name = "sender_id")
    private Long senderId;

    // 워크스페이스 관련 알림일 경우
    @Column(name = "workspace_id")
    private Long workspaceId;

    // 친구요청 id, 댓글 id 등 행위의 주체
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Builder
    public Notification(
            User user,
            NotificationType type,
            Long senderId,
            Long workspaceId,
            Long referenceId,
            Boolean isRead
    ) {
        this.user = user;
        this.type = type;
        this.senderId = senderId;
        this.workspaceId = workspaceId;
        this.referenceId = referenceId;
        this.isRead = isRead;
    }

    // 읽음 처리 메서드 추가
    public void markAsRead() {
        this.isRead = true;
    }
}
