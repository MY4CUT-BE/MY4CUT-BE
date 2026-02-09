package com.my4cut.domain.workspace.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.workspace.entity.Workspace;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WorkspaceRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .email("owner@test.com")
                .nickname("owner")
                .loginType(LoginType.EMAIL)
                .friendCode("OWNER123")
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(owner);

        // Active workspace
        Workspace active = Workspace.builder()
                .name("Active Workspace")
                .owner(owner)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        em.persist(active);

        // Expired workspace
        Workspace expired = Workspace.builder()
                .name("Expired Workspace")
                .owner(owner)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        em.persist(expired);

        // Deleted workspace
        Workspace deleted = Workspace.builder()
                .name("Deleted Workspace")
                .owner(owner)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        deleted.setDeletedAt(LocalDateTime.now());
        em.persist(deleted);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("만료되지 않고 삭제되지 않은 워크스페이스만 조회한다")
    void findActiveWorkspaces_Success() {
        // when
        List<Workspace> activeWorkspaces = workspaceRepository.findAllByExpiresAtAfterAndDeletedAtIsNull(LocalDateTime.now());

        // then
        assertThat(activeWorkspaces).hasSize(1);
        assertThat(activeWorkspaces.get(0).getName()).isEqualTo("Active Workspace");
    }
}
