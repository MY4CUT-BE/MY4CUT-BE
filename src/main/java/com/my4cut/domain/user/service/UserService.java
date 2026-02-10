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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String PROFILE_DIRECTORY = "profile";

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

        String profileImageViewUrl = imageStorageService.generatePresignedGetUrl(user.getProfileImageUrl());

        return UserResDTO.MeDTO.from(user, profileImageViewUrl, thisMonthDay4CutCount);
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
            MultipartFile profileImage
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.USER_DELETED);
        }

        validateImageFile(profileImage);

        String currentProfileImageUrl = user.getProfileImageUrl();
        String uploadedFileKey = imageStorageService.upload(profileImage, PROFILE_DIRECTORY);

        if (currentProfileImageUrl != null
                && !currentProfileImageUrl.isBlank()
                && !currentProfileImageUrl.equals(uploadedFileKey)) {
            imageStorageService.deleteIfExists(currentProfileImageUrl);
        }

        user.updateProfileImage(uploadedFileKey);

        return new UserResDTO.UpdateProfileImageDTO(
                uploadedFileKey,
                imageStorageService.generatePresignedGetUrl(uploadedFileKey)
        );
    }

    private void validateImageFile(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }
}
