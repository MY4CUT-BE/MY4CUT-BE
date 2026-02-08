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
    private static final String MEDIA_DIRECTORY = "calendar";

    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    // 미디어 파일 업로드
    @Transactional
    public MediaResDto.UploadResDto uploadMedia(Long userId, MultipartFile file) {
        User user = getUser(userId);
        MediaFile savedMediaFile = saveMediaFile(user, file);

        return MediaResDto.UploadResDto.of(
                savedMediaFile,
                imageStorageService.generatePresignedGetUrl(savedMediaFile.getFileUrl())
        );
    }

    @Transactional
    public List<MediaResDto.UploadResDto> uploadMediaBulk(Long userId, List<MultipartFile> files) {
        User user = getUser(userId);

        // 한 요청에서 여러 이미지를 받을 수 있어야 한다는 요구사항 반영
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        return files.stream()
                .map(file -> {
                    MediaFile saved = saveMediaFile(user, file);
                    return MediaResDto.UploadResDto.of(
                            saved,
                            imageStorageService.generatePresignedGetUrl(saved.getFileUrl())
                    );
                })
                .toList();
    }

    // 내 미디어 목록 조회
    @Transactional(readOnly = true)
    public List<MediaResDto.MediaListResDto> getMyMediaList(Long userId, int page) {
        User user = getUser(userId);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MediaFile> mediaFiles = mediaFileRepository.findAllByUploader(user, pageable);

        return mediaFiles.getContent().stream()
                .map(mediaFile -> MediaResDto.MediaListResDto.of(
                        mediaFile,
                        // DB에는 fileKey만 저장하고, 조회 시점에만 Presigned GET URL을 생성한다.
                        imageStorageService.generatePresignedGetUrl(mediaFile.getFileUrl())
                ))
                .toList();
    }

    // 미디어 상세 조회
    @Transactional(readOnly = true)
    public MediaResDto.MediaDetailResDto getMediaDetail(Long userId, Long mediaId) {
        getUser(userId);

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return MediaResDto.MediaDetailResDto.of(
                mediaFile,
                imageStorageService.generatePresignedGetUrl(mediaFile.getFileUrl())
        );
    }

    // ✅ 미디어 삭제 (수정된 부분)
    @Transactional
    public MediaResDto.DeleteResDto deleteMedia(Long userId, Long mediaId) {
        getUser(userId);

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        imageStorageService.deleteIfExists(mediaFile.getFileUrl());

        mediaFileRepository.delete(mediaFile);
        return MediaResDto.DeleteResDto.of(true);
    }
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private MediaFile saveMediaFile(User user, MultipartFile file) {
        // 서버가 multipart 파일을 직접 받아 S3에 업로드하고, 결과로 fileKey를 돌려받는다.
        String fileKey = imageStorageService.upload(file, MEDIA_DIRECTORY);
        MediaType mediaType = determineMediaType(file.getContentType());

        MediaFile mediaFile = MediaFile.builder()
                .uploader(user)
                .mediaType(mediaType)
                .fileUrl(fileKey)
                .isFinal(false)
                .build();

        return mediaFileRepository.save(mediaFile);
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.PHOTO;
    }
}
