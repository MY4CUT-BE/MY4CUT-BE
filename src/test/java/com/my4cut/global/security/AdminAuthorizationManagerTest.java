package com.my4cut.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAuthorizationManagerTest {

    @Test
    void parsesConfiguredAdminUserIds() {
        AdminAuthorizationManager manager = new AdminAuthorizationManager("1, 42,42");

        assertThat(manager.isAdmin(1L)).isTrue();
        assertThat(manager.isAdmin(42L)).isTrue();
        assertThat(manager.isAdmin(2L)).isFalse();
    }

    @Test
    void emptyConfigurationDeniesEveryUser() {
        AdminAuthorizationManager manager = new AdminAuthorizationManager("");

        assertThat(manager.isAdmin(1L)).isFalse();
    }

    @Test
    void invalidConfigurationFailsFast() {
        assertThatThrownBy(() -> new AdminAuthorizationManager("1,not-a-number"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void grantsOnlyAuthenticatedLongPrincipalInAdminList() {
        AdminAuthorizationManager manager = new AdminAuthorizationManager("1");
        UsernamePasswordAuthenticationToken admin =
                new UsernamePasswordAuthenticationToken(1L, null, List.of());
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(2L, null, List.of());

        assertThat(manager.check(() -> admin, null).isGranted()).isTrue();
        assertThat(manager.check(() -> user, null).isGranted()).isFalse();
    }
}
