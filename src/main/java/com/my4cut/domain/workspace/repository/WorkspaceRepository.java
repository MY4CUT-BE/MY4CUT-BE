package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByIdAndDeletedAtIsNull(Long id);
    List<Workspace> findAllByExpiresAtAfterAndDeletedAtIsNull(LocalDateTime now);
}
