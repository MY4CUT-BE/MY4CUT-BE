package com.my4cut.domain.tutorial.service;

import com.my4cut.domain.tutorial.dto.TutorialStatusResponseDto;
import com.my4cut.domain.tutorial.entity.UserTutorial;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.tutorial.repository.UserTutorialRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialService {

    private final UserTutorialRepository userTutorialRepository;
    private final UserRepository userRepository;

    @Transactional
    public void initialize(User user) {
        userTutorialRepository.findByUserId(user.getId())
                .orElseGet(() -> userTutorialRepository.save(new UserTutorial(user)));
    }

    @Transactional
    public TutorialStatusResponseDto getStatus(Long userId) {
        return TutorialStatusResponseDto.from(getOrCreate(userId));
    }

    @Transactional
    public TutorialStatusResponseDto complete(Long userId, TutorialType tutorialType) {
        UserTutorial tutorial = getOrCreate(userId);
        tutorial.complete(tutorialType);
        return TutorialStatusResponseDto.from(tutorial);
    }

    private UserTutorial getOrCreate(Long userId) {
        return userTutorialRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return userTutorialRepository.save(new UserTutorial(user));
                });
    }
}
