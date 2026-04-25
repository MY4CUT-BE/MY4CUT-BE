package com.my4cut.domain.image.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProfileImageUrlService {

    private final ImageStorageService imageStorageService;
    private final String publicBaseUrl;

    public ProfileImageUrlService(
            ImageStorageService imageStorageService,
            @Value("${app.public-base-url:}") String publicBaseUrl,
            @Value("${server.port:8080}") String serverPort
    ) {
        this.imageStorageService = imageStorageService;
        String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost:" + serverPort
                : publicBaseUrl;
        this.publicBaseUrl = trimTrailingSlash(baseUrl);
    }

    public String toResponseUrl(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return profileImageUrl;
        }

        if (isAbsoluteUrl(profileImageUrl)) {
            return profileImageUrl;
        }

        String viewUrl = imageStorageService.generatePresignedGetUrl(profileImageUrl);
        if (viewUrl == null || viewUrl.isBlank()) {
            return viewUrl;
        }

        if (isAbsoluteUrl(viewUrl)) {
            return viewUrl;
        }

        String imagePath = viewUrl.startsWith("/")
                ? viewUrl
                : "/images/" + viewUrl;
        return publicBaseUrl + imagePath;
    }

    private boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
