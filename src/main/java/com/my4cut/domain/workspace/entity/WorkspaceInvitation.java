package com.my4cut.domain.workspace.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public WorkspaceInvitation(Workspace workspace, User invitee, User inviter) {
        this.workspace = workspace;
        this.invitee = invitee;
        this.inviter = inviter;
        this.status = InvitationStatus.PENDING;
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
