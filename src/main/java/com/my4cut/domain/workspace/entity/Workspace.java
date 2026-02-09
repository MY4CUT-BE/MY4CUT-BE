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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Workspace(String name, User owner, LocalDateTime expiresAt) {
        this.name = name;
        this.owner = owner;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
