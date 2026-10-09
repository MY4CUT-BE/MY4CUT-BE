package com.my4cut.domain.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TutorialImageProperties {

    private static final String PROTECTED_PREFIX = "tutorial/";

    private final String workspaceImageKey;

    public TutorialImageProperties(
            @Value("${tutorial.workspace.image-key}") String workspaceImageKey
    ) {
        if (!StringUtils.hasText(workspaceImageKey)
                || !workspaceImageKey.startsWith(PROTECTED_PREFIX)
                || workspaceImageKey.contains("..")) {
            throw new IllegalArgumentException("tutorial workspace image key must be under tutorial/");
        }
        this.workspaceImageKey = workspaceImageKey;
    }

    public String workspaceImageKey() {
        return workspaceImageKey;
    }

    public String protectedPrefix() {
        return PROTECTED_PREFIX;
    }

    public boolean isProtected(String candidateKey) {
        return candidateKey != null && candidateKey.startsWith(PROTECTED_PREFIX);
    }
}
