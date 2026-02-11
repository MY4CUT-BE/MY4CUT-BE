package com.my4cut.domain.workspace.service;

import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.media.entity.MediaComment;
import com.my4cut.domain.media.repository.MediaCommentRepository;
import com.my4cut.domain.workspace.dto.*;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceMemberRepository;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 워크스페이스 사진 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * @author koohyunmo
 * @since 2026-02-08
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspacePhotoService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MediaFileRepository mediaFileRepository; // TODO: MediaFileService로 변경 필요
    private final MediaCommentRepository mediaCommentRepository;
    private final UserRepository userRepository; // TODO: UserService로 변경 필요
    private final ImageStorageService imageStorageService;

    /**
     * 워크스페이스에 사진(미디어)을 업로드/연결합니다.
     * @param workspaceId 워크스페이스 ID
     * @param requestDto 업로드할 미디어 ID 리스트를 담은 DTO
     * @param userId 유저 ID
     * @return 업로드된 사진 응답 DTO 리스트
     */
    @Transactional
    public List<WorkspacePhotoResponseDto> uploadPhotos(Long workspaceId,
            WorkspacePhotoUploadRequestDto requestDto,
            Long userId) {

        Workspace workspace = validateMembership(workspaceId, userId);
 
        List<MediaFile> updatedMediaFiles = new ArrayList<>();
 
        for (Long mediaId : requestDto.mediaIds()) {
            if (mediaId == null) {
                throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND); 
            }
            MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                    .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));
 
            // 업로더 본인인지 확인 (타인의 미디어를 본인 워크스페이스에 넣는 것 방지)
            if (!mediaFile.getUploader().getId().equals(userId)) {
                throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND); 
            }
 
            // 이미 워크스페이스가 배정되어 있는지 확인
            if (mediaFile.getWorkspace() != null) {
                throw new WorkspaceException(WorkspaceErrorCode.MEDIA_ALREADY_ASSIGNED);
            }
 
            // 워크스페이스 연결
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

    /**
     * 워크스페이스의 특정 사진을 삭제합니다.
     * @param workspaceId 워크스페이스 ID
     * @param photoId 사진(미디어) ID
     * @param userId 유저 ID
     */
    @Transactional
    public void deletePhoto(Long workspaceId, Long photoId, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);

        MediaFile photo = mediaFileRepository.findById(photoId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getWorkspace().getId().equals(workspaceId)) {
            throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND); // 다른 워크스페이스의 사진인 경우에도 NOT_FOUND 처리
        }

        // 업로더 본인인지 확인
        if (!photo.getUploader().getId().equals(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_PHOTO_OWNER);
        }

        // S3 이미지 제거
        imageStorageService.deleteIfExists(photo.getFileUrl());

        // DB 레코드 삭제
        mediaFileRepository.delete(photo);
    }

    /**
     * 워크스페이스의 사진 목록을 조회합니다.
     * @param workspaceId 워크스페이스 ID
     * @param sort 정렬 기준 (latest, oldest)
     * @param userId 유저 ID
     * @return 사진 응답 DTO 리스트
     */
    public List<WorkspacePhotoResponseDto> getPhotos(Long workspaceId, String sort, Long userId) {
        workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        validateMembership(workspaceId, userId);

        Sort sorting = sort.equalsIgnoreCase("oldest")
                ? Sort.by(Sort.Direction.ASC, "takenDate", "createdAt")
                : Sort.by(Sort.Direction.DESC, "takenDate", "createdAt");

        List<MediaFile> photos = mediaFileRepository.findAllByWorkspaceIdAndMediaType(workspaceId, MediaType.PHOTO,
                sorting);

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

    private Workspace validateMembership(Long workspaceId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        if (workspace.isExpired()) {
            throw new WorkspaceException(WorkspaceErrorCode.WORKSPACE_EXPIRED);
        }

        if (workspaceMemberRepository.findByWorkspaceAndUser(workspace, user).isEmpty()) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        return workspace;
    }

    /**
     * 사진의 댓글 목록을 조회합니다.
     * @param workspaceId 워크스페이스 ID
     * @param photoId 사진 ID
     * @param userId 유저 ID
     * @return 댓글 응답 DTO 리스트
     */
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

    /**
     * 특정 댓글을 삭제합니다.
     * @param workspaceId 워크스페이스 ID
     * @param photoId 사진 ID
     * @param commentId 삭제할 댓글 ID
     * @param userId 유저 ID
     */
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

    /**
     * 사진에 댓글을 등록합니다.
     * @param workspaceId 워크스페이스 ID
     * @param photoId 사진 ID
     * @param dto 댓글 내용 DTO
     * @param userId 유저 ID
     */
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

    private MediaFile validatePhotoInWorkspace(Long workspaceId, Long photoId) {
        MediaFile photo = mediaFileRepository.findById(photoId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getWorkspace().getId().equals(workspaceId)) {
            throw new WorkspaceException(WorkspaceErrorCode.PHOTO_NOT_FOUND);
        }
        return photo;
    }
}
