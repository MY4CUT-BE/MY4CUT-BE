package com.my4cut.domain.workspace.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공유 워크스페이스 정보를 저장하는 엔티티이다.
 * 사용자들이 함께 사진을 공유하고 관리할 수 있는 공간을 나타낸다.
 */
@Entity
@Table(name = "workspaces")
@Getter
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

    /**
     * Create a Workspace with the specified name, owner, and optional expiration time.
     *
     * @param name      the workspace name; must not be null
     * @param owner     the User who owns the workspace; must not be null
     * @param expiresAt the expiration timestamp for the workspace, or `null` if it does not expire
     */
    @Builder
    public Workspace(String name, User owner, LocalDateTime expiresAt) {
        this.name = name;
        this.owner = owner;
        this.expiresAt = expiresAt;
    }
}