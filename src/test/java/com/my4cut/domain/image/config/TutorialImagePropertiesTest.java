package com.my4cut.domain.image.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TutorialImagePropertiesTest {

    @Test
    void protectsEveryImageUnderTutorialPrefix() {
        TutorialImageProperties properties =
                new TutorialImageProperties("tutorial/retouch/default-photo.png");

        assertThat(properties.isProtected("tutorial/retouch/previous-photo.png")).isTrue();
        assertThat(properties.isProtected("calendar/user-photo.png")).isFalse();
    }

    @Test
    void rejectsImageKeyOutsideTutorialPrefix() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TutorialImageProperties("calendar/default-photo.png"));
    }
}
