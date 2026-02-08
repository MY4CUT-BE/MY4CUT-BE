package com.my4cut.domain.image.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
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

    private static final String UPLOAD_ROOT = System.getProperty("user.dir") + "/uploads";

    @Override
    public String upload(MultipartFile file) {
        return upload(file, "profile");
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        if (directory == null || directory.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            originalFilename = Paths.get(originalFilename).getFileName().toString();
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        String filename = UUID.randomUUID() + "_" + originalFilename;
        Path path = Paths.get(UPLOAD_ROOT, directory, filename);

        try {
            Files.createDirectories(path.getParent());
            file.transferTo(path.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, e);
        }

        return "/images/" + directory + "/" + filename;
    }

    @Override
    public String generatePresignedGetUrl(String fileKey) {
        return fileKey;
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
            String filePath = imagePathOrUrl;
            if (filePath.startsWith("/images/")) {
                filePath = UPLOAD_ROOT + filePath.substring("/images".length());
            }
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete local image: {}", imagePathOrUrl, e);
        }
    }

    private boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
