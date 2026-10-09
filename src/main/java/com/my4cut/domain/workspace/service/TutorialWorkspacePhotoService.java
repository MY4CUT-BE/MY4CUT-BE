package com.my4cut.domain.workspace.service;

import com.my4cut.domain.image.config.TutorialImageProperties;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.media.repository.MediaObjectRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.entity.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialWorkspacePhotoService {

    private static final String IMAGE_CONTENT_TYPE = "image/png";

    private final MediaObjectRepository mediaObjectRepository;
    private final MediaFileRepository mediaFileRepository;
    private final TutorialImageProperties imageProperties;

    @Transactional
    public void createDefaultPhoto(User owner, Workspace workspace) {
        String imageKey = imageProperties.workspaceImageKey();

        MediaObject mediaObject = mediaObjectRepository.save(
                MediaObject.builder()
                        .owner(owner)
                        .fileKey(imageKey)
                        .contentType(IMAGE_CONTENT_TYPE)
                        .status(MediaObjectStatus.ACTIVE)
                        .build()
        );

        mediaFileRepository.save(
                MediaFile.builder()
                        .uploader(owner)
                        .workspace(workspace)
                        .mediaObject(mediaObject)
                        .mediaType(MediaType.PHOTO)
                        .fileUrl(imageKey)
                        .isFinal(false)
                        .build()
        );
    }
}
