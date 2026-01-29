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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // fileName 화이트리스트 (URL-safe)
    private static final Pattern SAFE_FILENAME =
            Pattern.compile("^[A-Za-z0-9._-]+$");

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
        String safeFileName = sanitizeFileName(fileName);
        String uuid = UUID.randomUUID().toString();

        return switch (type) {
            case PROFILE -> "profile/" + uuid + "_" + safeFileName;
            case CALENDAR -> "calendar/" + uuid + "_" + safeFileName;
        };
    }

    /**
     * fileName 보안 정제
     * - path traversal 차단
     * - URL unsafe 문자 차단
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is blank.");
        }

        // 유니코드 정규화 (유사 문자 공격 방지)
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFKC);

        // 경로 구분자 제거
        normalized = normalized.replace("/", "")
                .replace("\\", "");

        if (!SAFE_FILENAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Unsupported fileName format.");
        }

        return normalized;
    }

    /**
     * S3 fileUrl → key 추출
     * - substring 방식 제거
     * - query parameter 무시
     * - URL 인코딩 대응
     */
    private String extractKey(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);

            String host = uri.getHost();
            if (host == null || !host.endsWith(".amazonaws.com")) {
                throw new IllegalArgumentException("Unsupported S3 host.");
            }

            String path = uri.getPath(); // /profile/xxx.jpg
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("Invalid S3 object path.");
            }

            String key = path.substring(1); // remove leading '/'
            return URLDecoder.decode(key, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid S3 file URL.", e);
        }
    }
}
