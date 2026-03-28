package com.my4cut.domain.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String upload(MultipartFile file);
    String upload(MultipartFile file, String directory);
    String upload(byte[] bytes, String originalFilename, String contentType, String directory);

    String generatePresignedGetUrl(String fileKey);

    /**
     * 저장소 타입에 맞게 fileKey 또는 URL 을 해석해 실제 파일 삭제를 시도한다.
     * true 는 삭제 성공 또는 이미 없는 상태를 뜻하고, false 는 삭제 실패를 뜻한다.
     */
    boolean deleteIfExists(String imagePathOrUrl);
}
