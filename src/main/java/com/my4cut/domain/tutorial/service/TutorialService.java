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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TutorialService {

    private final UserTutorialRepository userTutorialRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TutorialStatusResponseDto getStatus(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return TutorialStatusResponseDto.from(getCompletedTypes(userId));
    }

    @Transactional
    public TutorialStatusResponseDto complete(Long userId, TutorialType tutorialType) {
        User user = getUserForUpdate(userId);
        Set<TutorialType> completedTypes = getCompletedTypes(userId);

        if (completedTypes.add(tutorialType)) {
            userTutorialRepository.save(new UserTutorial(user, tutorialType));
        }

        return TutorialStatusResponseDto.from(completedTypes);
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<TutorialType> getCompletedTypes(Long userId) {
        List<UserTutorial> completedTutorials = userTutorialRepository.findAllByUserId(userId);
        EnumSet<TutorialType> completedTypes = EnumSet.noneOf(TutorialType.class);
        completedTutorials.stream()
                .map(UserTutorial::getTutorialType)
                .forEach(completedTypes::add);
        return completedTypes;
    }
}
