package com.my4cut.domain.workspace.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaComment;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaCommentRepository;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.media.service.MediaFileLifecycleService;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspacePhotoCommentRequestDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoCommentResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoUploadRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspacePhotoService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaCommentRepository mediaCommentRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final MediaFileLifecycleService mediaFileLifecycleService;

    @Transactional
    public List<WorkspacePhotoResponseDto> uploadPhotos(
            Long workspaceId,
            WorkspacePhotoUploadRequestDto requestDto,
            Long userId
    ) {
        Workspace workspace = validateMembership(workspaceId, userId);
        List<MediaFile> updatedMediaFiles = new ArrayList<>();

        for (Long mediaId : requestDto.mediaIds()) {
            if (mediaId == null) {
                throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
            }

            MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                    .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));

            if (!mediaFile.getUploader().getId().equals(userId)) {
                throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
            }

            // 사진 API 이므로 VIDEO 는 연결하지 않는다.
            if (mediaFile.getMediaType() != MediaType.PHOTO) {
                throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
            }

            if (mediaFile.getWorkspace() != null) {
                throw new WorkspaceException(WorkspaceErrorCode.MEDIA_ALREADY_ASSIGNED);
            }

            mediaFile.assignToWorkspace(workspace);
            updatedMediaFiles.add(mediaFile);
        }

        return updatedMediaFiles.stream()
                .map(file -> new WorkspacePhotoResponseDto(
                        file.getId(),
                        file.getFileUrl(),
                        imageStorageService.generatePresignedGetUrl(file.getFileUrl()),
                        file.getMediaType(),
                        file.getTakenDate(),
                        file.getIsFinal(),
                        file.getCreatedAt(),
                        file.getUploader().getNickname()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePhoto(Long workspaceId, Long photoId, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);

        MediaFile photo = mediaFileRepository.findById(photoId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));

        if (photo.getWorkspace() == null || !photo.getWorkspace().getId().equals(workspaceId)) {
            throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
        }

        if (!photo.getUploader().getId().equals(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_PHOTO_OWNER);
        }

        // 워크스페이스 사진도 공통 dedup 저장소를 쓰므로 같은 삭제 규칙을 적용한다.
        mediaFileLifecycleService.deleteMediaFile(photo);
    }

    public List<WorkspacePhotoResponseDto> getPhotos(Long workspaceId, String sort, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);

        Sort sorting = sort.equalsIgnoreCase("oldest")
                ? Sort.by(Sort.Direction.ASC, "takenDate", "createdAt")
                : Sort.by(Sort.Direction.DESC, "takenDate", "createdAt");

        List<MediaFile> photos = mediaFileRepository.findAllByWorkspaceIdAndMediaType(workspaceId, MediaType.PHOTO, sorting);

        return photos.stream()
                .map(photo -> new WorkspacePhotoResponseDto(
                        photo.getId(),
                        photo.getFileUrl(),
                        imageStorageService.generatePresignedGetUrl(photo.getFileUrl()),
                        photo.getMediaType(),
                        photo.getTakenDate(),
                        photo.getIsFinal(),
                        photo.getCreatedAt(),
                        photo.getUploader().getNickname()))
                .collect(Collectors.toList());
    }

    public List<WorkspacePhotoCommentResponseDto> getComments(Long workspaceId, Long photoId, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);
        validatePhotoInWorkspace(workspaceId, photoId);

        List<MediaComment> comments = mediaCommentRepository.findAllByMediaFileIdOrderByCreatedAtDesc(photoId);

        return comments.stream()
                .map(comment -> new WorkspacePhotoCommentResponseDto(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getNickname(),
                        comment.getUser().getProfileImageUrl(),
                        comment.getContent(),
                        comment.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Long workspaceId, Long photoId, Long commentId, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);
        validatePhotoInWorkspace(workspaceId, photoId);

        MediaComment comment = mediaCommentRepository.findById(commentId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_COMMENT_OWNER);
        }

        if (!comment.getMediaFile().getId().equals(photoId)) {
            throw new WorkspaceException(WorkspaceErrorCode.COMMENT_NOT_FOUND);
        }

        mediaCommentRepository.delete(comment);
    }

    @Transactional
    public void createComment(Long workspaceId, Long photoId, WorkspacePhotoCommentRequestDto dto, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);

        MediaFile mediaFile = validatePhotoInWorkspace(workspaceId, photoId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));

        MediaComment comment = MediaComment.builder()
                .mediaFile(mediaFile)
                .user(user)
                .content(dto.content())
                .build();

        mediaCommentRepository.save(comment);
    }

    private Workspace validateMembership(Long workspaceId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        if (workspace.isExpired()) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_EXPIRED);
        }

        if (workspaceMemberRepository.findByWorkspaceAndUser(workspace, user).isEmpty()) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        return workspace;
    }

    private MediaFile validatePhotoInWorkspace(Long workspaceId, Long photoId) {
        MediaFile photo = mediaFileRepository.findById(photoId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));

        if (photo.getWorkspace() == null || !photo.getWorkspace().getId().equals(workspaceId)) {
            throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
        }
        return photo;
    }
}
