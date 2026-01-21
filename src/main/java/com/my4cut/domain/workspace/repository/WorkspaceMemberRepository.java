package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    Optional<WorkspaceMember> findByWorkspaceAndUser(Workspace workspace, User user);

    void deleteByWorkspaceAndUser(Workspace workspace, User user);

    List<WorkspaceMember> findAllByUserId(Long userId);
}
