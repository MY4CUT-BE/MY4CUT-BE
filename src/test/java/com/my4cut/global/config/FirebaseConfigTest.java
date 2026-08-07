package com.my4cut.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseConfigTest {

    @Test
    void initialize_skipsMissingCredentialsWhenFirebaseIsDisabled() throws Exception {
        FirebaseConfig config = config(false, "");

        config.initialize();
    }

    @Test
    void initialize_failsFastWhenFirebaseIsEnabledWithoutCredentialsPath() {
        FirebaseConfig config = config(true, " ");

        assertThatThrownBy(config::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("firebase.enabled=true requires firebase.config-path.");
    }

    @Test
    void initialize_failsFastWhenCredentialsFileDoesNotExist() {
        FirebaseConfig config = config(true, "/not-found/firebase-service-account.json");

        assertThatThrownBy(config::initialize)
                .isInstanceOf(java.io.IOException.class);
    }

    private FirebaseConfig config(boolean enabled, String path) {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseEnabled", enabled);
        ReflectionTestUtils.setField(config, "firebaseConfigPath", path);
        return config;
    }
}
