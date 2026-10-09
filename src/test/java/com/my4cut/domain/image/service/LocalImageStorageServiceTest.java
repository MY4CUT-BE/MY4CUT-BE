package com.my4cut.domain.image.service;

import com.my4cut.domain.image.config.TutorialImageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImageStorageServiceTest {

    private static final String TUTORIAL_IMAGE_KEY = "tutorial/retouch/default-photo.png";

    private final LocalImageStorageService localImageStorageService =
            new LocalImageStorageService(new TutorialImageProperties(TUTORIAL_IMAGE_KEY));

    @Test
    void resolvesTutorialImageToLocallyServedUrl() {
        assertThat(localImageStorageService.generatePresignedGetUrl(TUTORIAL_IMAGE_KEY))
                .isEqualTo("/images/tutorial/retouch/default-photo.png");
        assertThat(new ClassPathResource("static/images/" + TUTORIAL_IMAGE_KEY).exists()).isTrue();
    }

    @Test
    void doesNotDeleteSharedTutorialImage() {
        assertThat(localImageStorageService.deleteIfExists(TUTORIAL_IMAGE_KEY)).isTrue();
        assertThat(localImageStorageService.deleteIfExists("/images/" + TUTORIAL_IMAGE_KEY)).isTrue();
    }
}
