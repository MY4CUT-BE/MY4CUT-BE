package com.my4cut.global.image;

import com.my4cut.global.image.dto.PresignedUrlReqDto;
import com.my4cut.global.image.dto.PresignedUrlResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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

    private String createKey(PresignedUrlReqDto.ImageType type, String fileName) {
        String uuid = UUID.randomUUID().toString();

        return switch (type) {
            case PROFILE -> "profile/" + uuid + "_" + fileName;
            case CALENDAR -> "calendar/" + uuid + "_" + fileName;
        };
    }
}
