package com.my4cut.domain.media.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class MediaHashService {

    public HashedMedia hash(MultipartFile file) {
        try {
            // 업로드 전에 서버에서 직접 SHA-256 을 계산해 dedup 기준으로 사용한다.
            byte[] bytes = file.getBytes();
            return new HashedMedia(
                    bytes,
                    toHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                    (long) bytes.length,
                    file.getContentType(),
                    file.getOriginalFilename()
            );
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    public record HashedMedia(
            // 같은 바이트를 업로드 단계에서도 재사용하기 위해 함께 보관한다.
            byte[] bytes,
            String sha256,
            Long fileSize,
            String contentType,
            String originalFilename
    ) {
    }
}
