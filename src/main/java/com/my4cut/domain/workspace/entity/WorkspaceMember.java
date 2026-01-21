package com.my4cut.domain.workspace.entity;

import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 워크스페이스 멤버 정보를 저장하는 엔티티이다.
 * 특정 워크스페이스에 참여한 사용자와 참여 시점을 관리한다.
 */
@Entity
@Table(name = "workspace_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    public WorkspaceMember(Workspace workspace, User user, LocalDateTime joinedAt) {
        this.workspace = workspace;
        this.user = user;
        this.joinedAt = joinedAt;
    }
}
