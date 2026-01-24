package com.my4cut.global.image;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
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
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // "/images/profile/xxx.jpg" → "uploads/profile/xxx.jpg"
        String relativePath = imageUrl.replace("/images/", "");
        Path path = Paths.get("uploads").resolve(relativePath);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 삭제 실패는 로그만 (서비스 중단 X)
            System.err.println("기존 이미지 삭제 실패: " + path);
        }
    }
}
