package com.my4cut.domain.image.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file) {
        return upload(file, "profile");
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), directory);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public String upload(byte[] bytes, String originalFilename, String contentType, String directory) {
        String key = buildKey(directory, originalFilename);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        return key;
    }

    @Override
    public String generatePresignedGetUrl(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }

        String key = extractKey(fileKey);
        if (key == null || key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    @Override
    public boolean deleteIfExists(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return true;
        }

        String key = extractKey(imagePathOrUrl);

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete S3 image: {}", imagePathOrUrl, e);
            return false;
        }
    }

    private String buildKey(String directory, String originalFilename) {
        if (directory == null || directory.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        String safeName = sanitizeFilename(originalFilename);
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return directory + "/" + yearMonth + "/" + UUID.randomUUID() + "_" + safeName;
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }

        String sanitized = originalFilename.replaceAll("[^A-Za-z0-9._-]", "");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    private String extractKey(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return imagePathOrUrl;
        }

        if (imagePathOrUrl.startsWith("http://") || imagePathOrUrl.startsWith("https://")) {
            try {
                URI uri = URI.create(imagePathOrUrl);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    return path.substring(1);
                }
            } catch (Exception e) {
                log.warn("Failed to parse image URL: {}", imagePathOrUrl, e);
            }
        }
        return imagePathOrUrl;
    }
}
