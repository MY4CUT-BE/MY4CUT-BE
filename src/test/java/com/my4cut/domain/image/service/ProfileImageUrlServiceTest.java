package com.my4cut.domain.image.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProfileImageUrlServiceTest {

    @Test
    void toResponseUrl_ConvertsImagePathToHttpUrl() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(imageStorageService, "", "8080");
        given(imageStorageService.generatePresignedGetUrl("/images/profile/user.png"))
                .willReturn("/images/profile/user.png");

        String result = service.toResponseUrl("/images/profile/user.png");

        assertThat(result).isEqualTo("http://localhost:8080/images/profile/user.png");
    }

    @Test
    void toResponseUrl_ConvertsLocalStorageKeyToHttpImageUrl() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(
                imageStorageService,
                "http://api.example.com/",
                "8080"
        );
        given(imageStorageService.generatePresignedGetUrl("profile/defaultProfile.png"))
                .willReturn("profile/defaultProfile.png");

        String result = service.toResponseUrl("profile/defaultProfile.png");

        assertThat(result).isEqualTo("http://api.example.com/images/profile/defaultProfile.png");
    }

    @Test
    void toResponseUrl_ReturnsS3PresignedUrl() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(imageStorageService, "", "8080");
        String presignedUrl = "https://my4cut-image-bucket.s3.ap-northeast-2.amazonaws.com/"
                + "profile/user.png?X-Amz-Signature=abc";
        given(imageStorageService.generatePresignedGetUrl("profile/user.png"))
                .willReturn(presignedUrl);

        String result = service.toResponseUrl("profile/user.png");

        assertThat(result).isEqualTo(presignedUrl);
    }

    @Test
    void toResponseUrl_KeepsAbsoluteUrl() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(imageStorageService, "", "8080");

        String result = service.toResponseUrl("https://cdn.example.com/profile.png");

        assertThat(result).isEqualTo("https://cdn.example.com/profile.png");
        verify(imageStorageService, never()).generatePresignedGetUrl("https://cdn.example.com/profile.png");
    }

    @Test
    void toResponseUrl_KeepsBlankValues() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(imageStorageService, "", "8080");

        assertThat(service.toResponseUrl(null)).isNull();
        assertThat(service.toResponseUrl("")).isEmpty();
        assertThat(service.toResponseUrl("   ")).isEqualTo("   ");
    }

    @Test
    void toResponseUrl_ReturnsBlankPresignedResult() {
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ProfileImageUrlService service = new ProfileImageUrlService(imageStorageService, "", "8080");
        given(imageStorageService.generatePresignedGetUrl("profile/missing.png")).willReturn(null);

        assertThat(service.toResponseUrl("profile/missing.png")).isNull();
    }
}
