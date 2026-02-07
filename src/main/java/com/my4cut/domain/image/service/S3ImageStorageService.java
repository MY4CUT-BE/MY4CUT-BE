package com.my4cut.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 프로필 이미지 업로드
     */
    @Override
    public String upload(MultipartFile file) {
        String key = "profile/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

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
            throw new RuntimeException("S3 이미지 업로드 실패", e);
        }

        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    /**
     * 이미지가 S3 URL이면 S3에서 삭제
     * - URL이 아니면 무시
     * - 다른 버킷이면 무시
     * - 실패해도 예외 던지지 않음
     */
    @Override
    public void deleteIfExists(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return;
        }

        if (!isS3Url(imagePathOrUrl)) {
            return;
        }

        try {
            S3Location location = parseS3Url(imagePathOrUrl);

            // 안전장치: 설정된 버킷만 삭제
            if (!bucket.equals(location.bucket())) {
                log.warn("Skip deleting S3 object from different bucket: {}", imagePathOrUrl);
                return;
            }

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(location.bucket())
                            .key(location.key())
                            .build()
            );

        } catch (Exception e) {
            log.warn("Failed to delete S3 image: {}", imagePathOrUrl, e);
        }
    }

    private boolean isS3Url(String url) {
        return url.startsWith("https://") && url.contains(".amazonaws.com/");
    }

    /**
     * https://bucket.s3.region.amazonaws.com/key 파싱
     */
    private S3Location parseS3Url(String url) {
        URI uri = URI.create(url);

        String host = uri.getHost(); // bucket.s3.ap-northeast-2.amazonaws.com
        String bucketName = host.split("\\.")[0];
        String key = uri.getPath().substring(1); // leading '/' 제거

        return new S3Location(bucketName, key);
    }

    private record S3Location(String bucket, String key) {}
}
