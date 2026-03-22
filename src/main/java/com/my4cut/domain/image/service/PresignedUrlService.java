package com.my4cut.domain.image.service;

import com.my4cut.domain.image.dto.PresignedUrlReqDto;
import com.my4cut.domain.image.dto.PresignedUrlResDto;
import com.my4cut.domain.image.dto.PresignedViewUrlReqDto;
import com.my4cut.domain.image.dto.PresignedViewUrlResDto;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public PresignedUrlResDto generate(Long userId, PresignedUrlReqDto dto) {
        // 현재 미디어 업로드는 서버가 multipart 를 직접 받아 처리하는 구조만 지원한다.
        throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    public PresignedViewUrlResDto generateViewUrl(PresignedViewUrlReqDto dto) {
        String key = dto.fileKey();

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

        return new PresignedViewUrlResDto(viewUrl, key);
    }
}
