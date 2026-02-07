package com.my4cut.domain.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
@Profile("local")
public class LocalImageStorageService implements ImageStorageService {

    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + "/uploads/profile";

    @Override
    public String upload(MultipartFile file) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR, filename);

        try {
            Files.createDirectories(path.getParent());
            file.transferTo(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패", e);
        }

        return "/images/profile/" + filename;
    }

    @Override
    public void deleteIfExists(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return;
        }

        // URL이면 로컬 삭제 대상 아님
        if (isUrl(imagePathOrUrl)) {
            return;
        }

        try {
            Path path = Paths.get(imagePathOrUrl);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete local image: {}", imagePathOrUrl, e);
        }
    }

    private boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
