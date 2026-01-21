package com.my4cut.domain.workspace.service;

import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoUploadRequestDto;
import com.my4cut.domain.workspace.entity.Workspace;
import com.my4cut.domain.workspace.exception.WorkspaceErrorCode;
import com.my4cut.domain.workspace.exception.WorkspaceException;
import com.my4cut.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 워크스페이스 사진 관련 비즈니스 로직을 처리하는 서비스 클래스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspacePhotoService {

    private final WorkspaceRepository workspaceRepository;
    private final MediaFileRepository mediaFileRepository; // TODO: MediaFileService로 변경 필요
    private final UserRepository userRepository; // TODO: UserService로 변경 필요

    /**
     * 워크스페이스에 사진을 업로드합니다.
     */
    @Transactional
    public List<WorkspacePhotoResponseDto> uploadPhotos(Long workspaceId,
            List<WorkspacePhotoUploadRequestDto> photoRequests,
            Long userId) {
        Workspace workspace = workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")); // 공통 유저 예외 적용 필요

        List<MediaFile> mediaFiles = new ArrayList<>();

        for (WorkspacePhotoUploadRequestDto photoRequest : photoRequests) {
            MultipartFile file = photoRequest.file();
            // TODO: 실제 S3 업로드 로직 구현
            // 현재는 더미 URL을 생성하여 저장합니다.
            String dummyUrl = "https://my4cut-bucket.s3.amazonaws.com/photos/" + file.getOriginalFilename();

            MediaFile mediaFile = MediaFile.builder()
                    .uploader(uploader)
                    .workspace(workspace)
                    .mediaType(MediaType.PHOTO)
                    .fileUrl(dummyUrl)
                    .takenDate(photoRequest.takenDate())
                    .isFinal(false)
                    .build();

            mediaFiles.add(mediaFile);
        }

        List<MediaFile> savedFiles = mediaFileRepository.saveAll(mediaFiles);

        return savedFiles.stream()
                .map(file -> new WorkspacePhotoResponseDto(
                        file.getId(),
                        file.getFileUrl()))
                .collect(Collectors.toList());
    }
}
