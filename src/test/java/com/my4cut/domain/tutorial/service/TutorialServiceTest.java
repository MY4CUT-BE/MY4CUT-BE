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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TutorialServiceTest {

    @Mock
    private UserTutorialRepository userTutorialRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TutorialService tutorialService;

    @Test
    void getStatus_returnsEveryTutorialAsIncompleteWhenNoProgressExists() {
        Long userId = 1L;
        User user = createUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userTutorialRepository.findAllByUserId(userId)).willReturn(List.of());

        TutorialStatusResponseDto result = tutorialService.getStatus(userId);

        assertThat(result.tutorials()).hasSize(TutorialType.values().length);
        assertThat(result.tutorials()).allMatch(status -> !status.completed());
    }

    @Test
    void complete_updatesOnlyRequestedTutorialAndIsIdempotent() {
        Long userId = 1L;
        User user = createUser();
        UserTutorial completedTutorial = new UserTutorial(user, TutorialType.HOME);
        given(userRepository.findByIdForUpdate(userId)).willReturn(Optional.of(user));
        given(userTutorialRepository.findAllByUserId(userId))
                .willReturn(List.of(), List.of(completedTutorial));

        tutorialService.complete(userId, TutorialType.HOME);
        TutorialStatusResponseDto result = tutorialService.complete(userId, TutorialType.HOME);

        assertThat(completed(result, TutorialType.HOME)).isTrue();
        assertThat(completed(result, TutorialType.UPLOAD_DATE)).isFalse();
        verify(userTutorialRepository, times(1)).save(any(UserTutorial.class));
    }

    private boolean completed(TutorialStatusResponseDto response, TutorialType type) {
        return response.tutorials().stream()
                .filter(status -> status.type() == type)
                .findFirst()
                .orElseThrow()
                .completed();
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
