package com.my4cut.domain.workspace.service;

import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.entity.MediaObject;
import com.my4cut.domain.image.config.TutorialImageProperties;
import com.my4cut.domain.media.enums.MediaObjectStatus;
import com.my4cut.domain.media.enums.MediaType;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.media.repository.MediaObjectRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.workspace.entity.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TutorialWorkspacePhotoServiceTest {

    private static final String IMAGE_KEY = "tutorial/retouch/default-photo.png";

    @Mock
    private MediaObjectRepository mediaObjectRepository;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private TutorialImageProperties imageProperties;

    @InjectMocks
    private TutorialWorkspacePhotoService tutorialWorkspacePhotoService;

    @Test
    void createsPhotoLinkedToDefaultWorkspace() {
        User owner = User.builder().nickname("owner").build();
        ReflectionTestUtils.setField(owner, "id", 1L);
        Workspace workspace = Workspace.builder().name("포토리의 스페이스").build();
        ReflectionTestUtils.setField(workspace, "id", 10L);

        given(imageProperties.workspaceImageKey()).willReturn(IMAGE_KEY);
        given(mediaObjectRepository.save(any(MediaObject.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        tutorialWorkspacePhotoService.createDefaultPhoto(owner, workspace);

        ArgumentCaptor<MediaObject> mediaObjectCaptor = ArgumentCaptor.forClass(MediaObject.class);
        verify(mediaObjectRepository).save(mediaObjectCaptor.capture());
        MediaObject mediaObject = mediaObjectCaptor.getValue();
        assertThat(mediaObject.getOwner()).isSameAs(owner);
        assertThat(mediaObject.getFileKey()).isEqualTo(IMAGE_KEY);
        assertThat(mediaObject.getContentType()).isEqualTo("image/png");
        assertThat(mediaObject.getStatus()).isEqualTo(MediaObjectStatus.ACTIVE);

        ArgumentCaptor<MediaFile> mediaFileCaptor = ArgumentCaptor.forClass(MediaFile.class);
        verify(mediaFileRepository).save(mediaFileCaptor.capture());
        MediaFile mediaFile = mediaFileCaptor.getValue();
        assertThat(mediaFile.getUploader()).isSameAs(owner);
        assertThat(mediaFile.getWorkspace()).isSameAs(workspace);
        assertThat(mediaFile.getMediaObject()).isSameAs(mediaObject);
        assertThat(mediaFile.getMediaType()).isEqualTo(MediaType.PHOTO);
        assertThat(mediaFile.getFileUrl()).isEqualTo(IMAGE_KEY);
        assertThat(mediaFile.getIsFinal()).isFalse();
    }
}
