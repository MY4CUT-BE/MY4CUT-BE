package com.my4cut.global.image;

import com.my4cut.global.image.dto.PresignedUrlReqDto;
import com.my4cut.global.image.dto.PresignedUrlResDto;
import com.my4cut.global.image.dto.PresignedViewUrlReqDto;
import com.my4cut.global.image.dto.PresignedViewUrlResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public PresignedUrlResDto generate(PresignedUrlReqDto dto) {

        String key = createKey(dto.type(), dto.fileName());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(dto.contentType())
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .putObjectRequest(putObjectRequest)
                        .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        String fileUrl = "https://" + bucket + ".s3.amazonaws.com/" + key;

        return new PresignedUrlResDto(uploadUrl, fileUrl);
    }

    public PresignedViewUrlResDto generateViewUrl(PresignedViewUrlReqDto dto) {
        String key = extractKey(dto.fileUrl());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(getObjectRequest)
                        .build();

        String viewUrl = s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();

        return new PresignedViewUrlResDto(viewUrl, dto.fileUrl());
    }

    private String createKey(PresignedUrlReqDto.ImageType type, String fileName) {
        String uuid = UUID.randomUUID().toString();

        return switch (type) {
            case PROFILE -> "profile/" + uuid + "_" + fileName;
            case CALENDAR -> "calendar/" + uuid + "_" + fileName;
        };
    }

    private String extractKey(String fileUrl) {
        int keyIndex = fileUrl.indexOf(".amazonaws.com/");
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Unsupported S3 file URL format.");
        }
        return fileUrl.substring(keyIndex + ".amazonaws.com/".length());
    }
}