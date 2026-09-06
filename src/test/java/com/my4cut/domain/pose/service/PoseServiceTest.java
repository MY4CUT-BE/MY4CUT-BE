package com.my4cut.domain.pose.service;

import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.pose.dto.res.PoseResDto;
import com.my4cut.domain.pose.entity.Pose;
import com.my4cut.domain.pose.repository.PoseFavoriteRepository;
import com.my4cut.domain.pose.repository.PoseRepository;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PoseServiceTest {

    @Mock
    private PoseRepository poseRepository;
    @Mock
    private PoseFavoriteRepository poseFavoriteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private PoseService poseService;

    @Test
    void returnsBookmarkStatusForEachPoseInList() {
        Pose favoritePose = pose(1L, "즐겨찾기 포즈", "poses/favorite.jpg");
        Pose normalPose = pose(2L, "일반 포즈", "poses/normal.jpg");
        given(poseRepository.findAll(any(Sort.class)))
                .willReturn(List.of(favoritePose, normalPose));
        given(poseFavoriteRepository.findFavoritePoseIds(10L, List.of(1L, 2L)))
                .willReturn(List.of(1L));
        given(imageStorageService.generatePresignedGetUrl("poses/favorite.jpg"))
                .willReturn("favorite-view-url");
        given(imageStorageService.generatePresignedGetUrl("poses/normal.jpg"))
                .willReturn("normal-view-url");

        List<PoseResDto.PoseListResDto> result = poseService.getPoseList(10L, null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBookmarked()).isTrue();
        assertThat(result.get(1).getBookmarked()).isFalse();
        verify(poseFavoriteRepository).findFavoritePoseIds(10L, List.of(1L, 2L));
    }

    @Test
    void skipsFavoriteQueryWhenPoseListIsEmpty() {
        given(poseRepository.findAll(any(Sort.class))).willReturn(List.of());

        List<PoseResDto.PoseListResDto> result = poseService.getPoseList(10L, null, null);

        assertThat(result).isEmpty();
        verify(poseFavoriteRepository, never()).findFavoritePoseIds(any(), any());
    }

    @Test
    void returnsBookmarkedTrueForFavoritePoseDetail() {
        Pose pose = pose(1L, "즐겨찾기 포즈", "poses/favorite.jpg");
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));
        given(poseFavoriteRepository.existsByUserIdAndPoseId(10L, 1L)).willReturn(true);
        given(imageStorageService.generatePresignedGetUrl("poses/favorite.jpg"))
                .willReturn("favorite-view-url");

        PoseResDto.PoseDetailResDto result = poseService.getPoseDetail(10L, 1L);

        assertThat(result.getBookmarked()).isTrue();
    }

    @Test
    void returnsBookmarkedFalseForNonFavoritePoseDetail() {
        Pose pose = pose(1L, "일반 포즈", "poses/normal.jpg");
        given(poseRepository.findById(1L)).willReturn(Optional.of(pose));
        given(poseFavoriteRepository.existsByUserIdAndPoseId(10L, 1L)).willReturn(false);
        given(imageStorageService.generatePresignedGetUrl("poses/normal.jpg"))
                .willReturn("normal-view-url");

        PoseResDto.PoseDetailResDto result = poseService.getPoseDetail(10L, 1L);

        assertThat(result.getBookmarked()).isFalse();
    }

    private Pose pose(Long id, String title, String imageUrl) {
        Pose pose = Pose.builder()
                .title(title)
                .imageUrl(imageUrl)
                .peopleCount(2)
                .build();
        ReflectionTestUtils.setField(pose, "id", id);
        return pose;
    }
}
