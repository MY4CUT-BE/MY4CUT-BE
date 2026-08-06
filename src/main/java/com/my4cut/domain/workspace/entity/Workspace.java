package com.my4cut.domain.workspace.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공유 워크스페이스 정보를 저장하는 엔티티이다.
 * 사용자들이 함께 사진을 공유하고 관리할 수 있는 공간을 나타낸다.
 */
@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * 기존 owner_id 스키마와의 배포 호환성을 위해 생성자 참조만 유지한다.
     * 권한 판정이나 API 응답에는 사용하지 않는다.
     */
    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User creator;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Workspace(String name, User creator, LocalDateTime expiresAt) {
        this.name = name;
        this.creator = creator;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
