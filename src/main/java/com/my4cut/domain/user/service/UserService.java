package com.my4cut.domain.user.service;

import com.my4cut.domain.day4cut.repository.Day4CutRepository;
import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.domain.image.service.ImageStorageService;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final Day4CutRepository day4CutRepository;
    @Transactional(readOnly = true)
    public UserResDTO.MeDTO getMyInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        long thisMonthDay4CutCount = day4CutRepository.countByUserAndDateBetween(user, startOfMonth, endOfMonth);

        return UserResDTO.MeDTO.from(user, thisMonthDay4CutCount);
    }

    //닉네임 변경
    @Transactional
    public UserResDTO.UpdateNicknameDTO updateNickname(
            Long userId,
            UserReqDTO.UpdateNicknameDTO request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        user.updateNickname(request.nickname());

        return new UserResDTO.UpdateNicknameDTO(user.getNickname());
    }

    //프로필 사진 변경
    @Transactional
    public UserResDTO.UpdateProfileImageDTO updateProfileImage(
            Long userId,
            UserReqDTO.UpdateProfileImageDTO request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        String newProfileImageUrl = request.profileImageUrl();
        String currentProfileImageUrl = user.getProfileImageUrl();

        if (currentProfileImageUrl != null
                && !currentProfileImageUrl.isBlank()
                && !currentProfileImageUrl.equals(newProfileImageUrl)) {

            imageStorageService.deleteIfExists(currentProfileImageUrl);
        }

        user.updateProfileImage(newProfileImageUrl);

        return new UserResDTO.UpdateProfileImageDTO(newProfileImageUrl);
    }
}
