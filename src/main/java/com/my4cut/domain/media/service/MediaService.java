package com.my4cut.domain.media.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.dto.res.MediaResDto;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    private static final String MEDIA_DIRECTORY = "calendar";
    private static final int MAX_BULK_UPLOAD_COUNT = 10;

    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final MediaFileLifecycleService mediaFileLifecycleService;

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

        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (files.size() > MAX_BULK_UPLOAD_COUNT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        List<MediaFile> uploadedMediaFiles = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                uploadedMediaFiles.add(saveMediaFile(user, file));
            }
        } catch (Exception e) {
            for (MediaFile media : uploadedMediaFiles) {
                try {
                    mediaFileLifecycleService.deleteMediaFile(media);
                } catch (Exception deleteEx) {
                    log.warn("Bulk upload compensation failed: {}", media.getFileUrl(), deleteEx);
                }
            }
            throw e;
        }

        return uploadedMediaFiles.stream()
                .map(media -> MediaResDto.UploadResDto.of(
                        media,
                        imageStorageService.generatePresignedGetUrl(media.getFileUrl())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResDto.MediaListResDto> getMyMediaList(Long userId, int page) {
        User user = getUser(userId);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MediaFile> mediaFiles = mediaFileRepository.findAllByUploader(user, pageable);

        return mediaFiles.getContent().stream()
                .map(mediaFile -> MediaResDto.MediaListResDto.of(
                        mediaFile,
                        imageStorageService.generatePresignedGetUrl(mediaFile.getFileUrl())
                ))
                .toList();
    }

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

    @Transactional
    public MediaResDto.DeleteResDto deleteMedia(Long userId, Long mediaId) {
        getUser(userId);

        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!mediaFile.getUploader().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        mediaFileLifecycleService.deleteMediaFile(mediaFile);
        return MediaResDto.DeleteResDto.of(true);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private MediaFile saveMediaFile(User user, MultipartFile file) {
        validateContentType(file.getContentType());
        MediaType mediaType = determineMediaType(file.getContentType());
        // 업로드, dedup, physical delete 순서는 lifecycle 서비스에 위임한다.
        return mediaFileLifecycleService.createMediaFile(user, file, MEDIA_DIRECTORY, mediaType);
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.PHOTO;
    }

    private void validateContentType(String contentType) {
        if (contentType == null ||
                !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
