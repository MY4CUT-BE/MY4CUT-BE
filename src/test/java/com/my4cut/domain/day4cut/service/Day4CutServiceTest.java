package com.my4cut.domain.day4cut.service;

import com.my4cut.domain.day4cut.dto.req.Day4CutReqDto;
import com.my4cut.domain.day4cut.entity.Day4Cut;
import com.my4cut.domain.day4cut.repository.Day4CutRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.media.entity.MediaFile;
import com.my4cut.domain.media.repository.MediaFileRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Day4CutServiceTest {

    @Mock
    private Day4CutRepository day4CutRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private Day4CutService day4CutService;

    @Test
    @DisplayName("내용과 이모지 없이 사진만으로 하루네컷을 생성할 수 있다")
    void 하루네컷_생성_성공_내용과_이모지가_없는_경우() {
        // Arrange
        Long userId = 1L;
        Long mediaId = 10L;
        LocalDate date = LocalDate.of(2026, 6, 29);
        User user = org.mockito.Mockito.mock(User.class);
        MediaFile mediaFile = org.mockito.Mockito.mock(MediaFile.class);
        Day4CutReqDto.CreateReqDto request = new Day4CutReqDto.CreateReqDto(
                date,
                null,
                null,
                List.of(new Day4CutReqDto.ImageReqDto(mediaId, null))
        );

        given(user.getId()).willReturn(userId);
        given(user.getStatus()).willReturn(UserStatus.ACTIVE);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(day4CutRepository.existsByUserAndDate(user, date)).willReturn(false);
        given(mediaFileRepository.findById(mediaId)).willReturn(Optional.of(mediaFile));
        given(mediaFile.getUploader()).willReturn(user);

        // Act
        day4CutService.createDay4Cut(userId, request);

        // Assert
        ArgumentCaptor<Day4Cut> captor = ArgumentCaptor.forClass(Day4Cut.class);
        verify(day4CutRepository).save(captor.capture());
        Day4Cut savedDay4Cut = captor.getValue();

        assertThat(savedDay4Cut.getContent()).isNull();
        assertThat(savedDay4Cut.getEmojiType()).isNull();
        assertThat(savedDay4Cut.getImages()).hasSize(1);
        assertThat(savedDay4Cut.getImages().get(0).getIsThumbnail()).isTrue();
    }
}
