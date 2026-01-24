package com.my4cut.global.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String upload(MultipartFile file);
    //기존 이미지 제거
    void delete(String imageUrl);
}
