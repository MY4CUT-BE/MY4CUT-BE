package com.my4cut.domain.tutorial.service;

import com.my4cut.domain.tutorial.dto.TutorialStatusResponseDto;
import com.my4cut.domain.tutorial.entity.UserTutorial;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.tutorial.repository.UserTutorialRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TutorialServiceTest {

    @Mock
    private UserTutorialRepository userTutorialRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TutorialService tutorialService;

    @Test
    void getStatus_createsDefaultStatusWhenMissing() {
        Long userId = 1L;
        User user = createUser();
        given(userRepository.findByIdForUpdate(userId)).willReturn(Optional.of(user));
        given(userTutorialRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userTutorialRepository.save(any(UserTutorial.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TutorialStatusResponseDto result = tutorialService.getStatus(userId);

        assertThat(result.home()).isFalse();
        assertThat(result.workspace()).isFalse();
        assertThat(result.photoUpload()).isFalse();
    }

    @Test
    void complete_updatesOnlyRequestedTutorialAndIsIdempotent() {
        Long userId = 1L;
        UserTutorial tutorial = new UserTutorial(createUser());
        given(userRepository.findByIdForUpdate(userId)).willReturn(Optional.of(tutorial.getUser()));
        given(userTutorialRepository.findByUserId(userId)).willReturn(Optional.of(tutorial));

        tutorialService.complete(userId, TutorialType.HOME);
        TutorialStatusResponseDto result = tutorialService.complete(userId, TutorialType.HOME);

        assertThat(result.home()).isTrue();
        assertThat(result.workspace()).isFalse();
        assertThat(result.photoUpload()).isFalse();
    }

    private User createUser() {
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .nickname("user")
                .loginType(LoginType.EMAIL)
                .friendCode("ABC123")
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
