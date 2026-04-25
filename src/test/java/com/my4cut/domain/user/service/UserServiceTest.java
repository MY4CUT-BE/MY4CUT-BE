package com.my4cut.domain.user.service;

import com.my4cut.domain.day4cut.repository.Day4CutRepository;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.domain.image.service.ProfileImageUrlService;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private ProfileImageUrlService profileImageUrlService;
    @Mock private Day4CutRepository day4CutRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getMyInfo_ReturnsProfileImageViewUrl() {
        Long userId = 1L;
        User user = User.builder()
                .email("user@example.com")
                .nickname("user")
                .profileImageUrl("profile/user.png")
                .loginType(LoginType.EMAIL)
                .friendCode("ABC123")
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(day4CutRepository.countByUserAndDateBetween(
                any(User.class),
                any(LocalDate.class),
                any(LocalDate.class)))
                .willReturn(0L);
        given(profileImageUrlService.toResponseUrl("profile/user.png"))
                .willReturn("https://my4cut-image-bucket.s3.ap-northeast-2.amazonaws.com/profile/user.png");

        UserResDTO.MeDTO result = userService.getMyInfo(userId);

        assertThat(result.profileImageFileKey()).isEqualTo("profile/user.png");
        assertThat(result.profileImageViewUrl())
                .isEqualTo("https://my4cut-image-bucket.s3.ap-northeast-2.amazonaws.com/profile/user.png");
    }
}
