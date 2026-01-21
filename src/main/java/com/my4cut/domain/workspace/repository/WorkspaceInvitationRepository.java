package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.workspace.entity.WorkspaceInvitation;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    List<WorkspaceInvitation> findAllByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    Optional<WorkspaceInvitation> findByIdAndInviteeId(Long id, Long inviteeId);
}
