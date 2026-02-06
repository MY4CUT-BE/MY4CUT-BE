package com.my4cut.global.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String upload(MultipartFile file);
    //기존 이미지 제거
    //void delete(String imageUrl);

    /**
     * 로컬 이미지인 경우만 삭제하고,
     * URL(S3 등)인 경우는 아무 동작도 하지 않는다.
     */
    void deleteIfExists(String imagePathOrUrl);
}
