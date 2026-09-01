package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    List<WorkspaceInvitation> findAllByWorkspaceIdAndStatus(Long workspaceId, InvitationStatus status);

    List<WorkspaceInvitation> findAllByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    Optional<WorkspaceInvitation> findByIdAndInviteeId(Long id, Long inviteeId);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndInviteeIdAndStatus(Long workspaceId, Long inviteeId, InvitationStatus status);

    void deleteAllByWorkspaceIdAndStatus(Long workspaceId, InvitationStatus status);

    @Modifying
    @Query("""
            delete from WorkspaceInvitation invitation
            where invitation.status = :status
              and (invitation.invitee = :user or invitation.inviter = :user)
            """)
    int deleteAllPendingInvolvingUser(
            @Param("user") User user,
            @Param("status") InvitationStatus status
    );
}
