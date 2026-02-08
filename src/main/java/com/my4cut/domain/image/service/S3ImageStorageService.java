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
        String key = buildKey(directory, file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, e);
        }

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
    public void deleteIfExists(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return;
        }

        // 과거 데이터는 전체 S3 URL, 신규 데이터는 fileKey로 저장될 수 있어
        // 삭제 시점에는 둘 다 안전하게 지원한다.
        String key = extractKey(imagePathOrUrl);

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );

        } catch (Exception e) {
            log.warn("Failed to delete S3 image: {}", imagePathOrUrl, e);
        }
    }

    private String buildKey(String directory, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        // 요구사항에 맞춰 URL이 아닌 fileKey만 DB에 저장한다.
        // 예) calendar/2026/02/{uuid}_image.jpg
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
        if (imagePathOrUrl.startsWith("http://") || imagePathOrUrl.startsWith("https://")) {
            int domainEnd = imagePathOrUrl.indexOf(".amazonaws.com/");
            if (domainEnd > 0) {
                return imagePathOrUrl.substring(domainEnd + ".amazonaws.com/".length());
            }
        }
        return imagePathOrUrl;
    }
}
