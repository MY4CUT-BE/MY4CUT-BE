package com.my4cut.domain.image.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String upload(MultipartFile file);
    String upload(MultipartFile file, String directory);

    String generatePresignedGetUrl(String fileKey);

    /**
     * 저장소 타입에 맞게 fileKey(또는 레거시 URL)를 해석해 실제 파일을 삭제한다.
     */
    void deleteIfExists(String imagePathOrUrl);
}
