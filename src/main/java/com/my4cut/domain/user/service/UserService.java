package com.my4cut.domain.user.service;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.image.ImageStorageService;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    @Transactional(readOnly = true)
    public UserResDTO.MeDTO getMyInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return UserResDTO.MeDTO.from(user);
    }

    //닉네임 변경
    @Transactional
    public UserResDTO.UpdateNicknameDTO updateNickname(
            Long userId,
            UserReqDTO.UpdateNicknameDTO request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        user.updateNickname(request.nickname());

        return new UserResDTO.UpdateNicknameDTO(user.getNickname());
    }

    @Transactional
    public UserResDTO.UpdateProfileImageDTO updateProfileImage(
            Long userId,
            UserReqDTO.UpdateProfileImageDTO request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String newProfileImageUrl = request.profileImageUrl();
        String currentProfileImageUrl = user.getProfileImageUrl();
        if (currentProfileImageUrl != null
                && !currentProfileImageUrl.isBlank()
                && !currentProfileImageUrl.equals(newProfileImageUrl)) {
            // 1. 기존 이미지 삭제
            imageStorageService.delete(currentProfileImageUrl);
        }

        // 2. DB 반영
        user.updateProfileImage(newProfileImageUrl);

        return new UserResDTO.UpdateProfileImageDTO(newProfileImageUrl);
    }
}
