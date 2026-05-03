package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.workspace.entity.Workspace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from Workspace workspace where workspace.id = :id and workspace.deletedAt is null")
    Optional<Workspace> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    List<Workspace> findAllByExpiresAtAfterAndDeletedAtIsNull(LocalDateTime now);
}
