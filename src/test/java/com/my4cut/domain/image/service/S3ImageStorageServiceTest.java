package com.my4cut.domain.image.service;

import com.my4cut.domain.image.config.TutorialImageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceTest {

    private static final String TUTORIAL_IMAGE_KEY = "tutorial/retouch/default-photo.png";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private TutorialImageProperties tutorialImageProperties;

    @InjectMocks
    private S3ImageStorageService s3ImageStorageService;

    @Test
    void doesNotDeleteSharedTutorialImage() {
        given(tutorialImageProperties.isProtected(TUTORIAL_IMAGE_KEY)).willReturn(true);

        boolean deleted = s3ImageStorageService.deleteIfExists(TUTORIAL_IMAGE_KEY);

        assertThat(deleted).isTrue();
        verifyNoInteractions(s3Client);
    }
}
