package com.my4cut.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final Set<Long> adminUserIds;

    public AdminAuthorizationManager(@Value("${admin.user-ids:}") String configuredAdminUserIds) {
        this.adminUserIds = parseAdminUserIds(configuredAdminUserIds);
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context
    ) {
        Authentication authentication = authenticationSupplier.get();
        boolean granted = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long userId
                && adminUserIds.contains(userId);

        return new AuthorizationDecision(granted);
    }

    boolean isAdmin(Long userId) {
        return userId != null && adminUserIds.contains(userId);
    }

    private Set<Long> parseAdminUserIds(String configuredAdminUserIds) {
        if (configuredAdminUserIds == null || configuredAdminUserIds.isBlank()) {
            return Collections.emptySet();
        }

        try {
            return Arrays.stream(configuredAdminUserIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("ADMIN_USER_IDS must contain only comma-separated numbers.", exception);
        }
    }
}
