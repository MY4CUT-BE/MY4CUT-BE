package com.my4cut.domain.media.service;

import com.my4cut.domain.image.config.TutorialImageProperties;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private MediaFileLifecycleService mediaFileLifecycleService;

    @Mock
    private TutorialImageProperties tutorialImageProperties;

    @InjectMocks
    private MediaService mediaService;

    @Test
    void excludesTutorialWorkspaceImageFromPersonalMediaList() {
        User user = User.builder().nickname("owner").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(tutorialImageProperties.protectedPrefix()).willReturn("tutorial/");
        given(mediaFileRepository.findAllByUploaderExcludingFileUrlPrefix(any(), any(), any()))
                .willReturn(Page.empty());

        assertThat(mediaService.getMyMediaList(1L, 0)).isEmpty();

        verify(mediaFileRepository).findAllByUploaderExcludingFileUrlPrefix(
                user,
                "tutorial/",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }
}
