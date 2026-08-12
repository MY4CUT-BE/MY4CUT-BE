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
        User lockedUser = getUserForUpdate(user.getId());
        getOrCreate(lockedUser);
    }

    @Transactional
    public TutorialStatusResponseDto getStatus(Long userId) {
        User user = getUserForUpdate(userId);
        return TutorialStatusResponseDto.from(getOrCreate(user));
    }

    @Transactional
    public TutorialStatusResponseDto complete(Long userId, TutorialType tutorialType) {
        User user = getUserForUpdate(userId);
        UserTutorial tutorial = getOrCreate(user);
        tutorial.complete(tutorialType);
        return TutorialStatusResponseDto.from(tutorial);
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserTutorial getOrCreate(User user) {
        return userTutorialRepository.findByUserId(user.getId())
                .orElseGet(() -> userTutorialRepository.save(new UserTutorial(user)));
    }
}
