package com.my4cut.domain.media.service;

import com.my4cut.domain.media.dto.res.MediaResDto;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    // 미디어 파일 업로드
    @Transactional
    public MediaResDto.UploadResDto uploadMedia(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        String fileUrl = imageStorageService.upload(file);
        MediaType mediaType = determineMediaType(file.getContentType());

        MediaFile mediaFile = MediaFile.builder()
                .uploader(user)
                .mediaType(mediaType)
                .fileUrl(fileUrl)
                .isFinal(false)
                .build();

        MediaFile savedMediaFile = mediaFileRepository.save(mediaFile);
        return MediaResDto.UploadResDto.of(savedMediaFile);
    }

    // 내 미디어 목록 조회
    @Transactional(readOnly = true)
    public List<MediaResDto.MediaListResDto> getMyMediaList(Long userId, int page) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MediaFile> mediaFiles = mediaFileRepository.findAllByUploader(user, pageable);

        return mediaFiles.getContent().stream()
                .map(MediaResDto.MediaListResDto::of)
                .toList();
    }

    // 미디어 상세 조회
    @Transactional(readOnly = true)
    public MediaResDto.MediaDetailResDto getMediaDetail(Long userId, Long mediaId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return MediaResDto.MediaDetailResDto.of(mediaFile);
    }

    // ✅ 미디어 삭제 (수정된 부분)
    @Transactional
    public MediaResDto.DeleteResDto deleteMedia(Long userId, Long mediaId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 🔥 여기만 변경됨
        imageStorageService.deleteIfExists(mediaFile.getFileUrl());

        mediaFileRepository.delete(mediaFile);
        return MediaResDto.DeleteResDto.of(true);
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.PHOTO;
    }
}
