package com.my4cut.domain.image.service;

import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.image.dto.PresignedUrlReqDto;
import com.my4cut.global.image.dto.PresignedUrlResDto;
import com.my4cut.global.image.dto.PresignedViewUrlReqDto;
import com.my4cut.global.image.dto.PresignedViewUrlResDto;
import com.my4cut.global.response.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.text.Normalizer;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // fileName 화이트리스트 (URL-safe)
    private static final Pattern SAFE_FILENAME =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    @Transactional
    public PresignedUrlResDto generate(Long userId, PresignedUrlReqDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_OWNER_NOT_FOUND));
        String key = createKey(dto.type(), dto.fileName());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .putObjectRequest(putObjectRequest)
                        .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        MediaType mediaType = determineMediaType(dto.contentType());
        MediaFile mediaFile = MediaFile.builder()
                .uploader(user)
                .mediaType(mediaType)
                // URL 대신 fileKey만 저장해 CDN 도입 시에도 DB 마이그레이션이 필요 없도록 한다.
                .fileUrl(key)
                .isFinal(false)
                .build();

        MediaFile savedMediaFile = mediaFileRepository.save(mediaFile);

        return new PresignedUrlResDto(savedMediaFile.getId(), uploadUrl, key);
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
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FILE_NAME);
        }

        // 유니코드 정규화 (유사 문자 공격 방지)
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFKC);

        // 경로 구분자 제거
        normalized = normalized.replace("/", "")
                .replace("\\", "");

        if (!SAFE_FILENAME.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FILE_NAME);
        }

        return normalized;
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.PHOTO;
    }

}
