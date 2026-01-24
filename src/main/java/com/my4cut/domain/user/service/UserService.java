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
import org.springframework.web.multipart.MultipartFile;

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
            MultipartFile profileImage
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 1. 기존 이미지 삭제
        imageStorageService.delete(user.getProfileImageUrl());

        // 2. 새 이미지 업로드
        String imageUrl = imageStorageService.upload(profileImage);

        // 3. DB 반영
        user.updateProfileImage(imageUrl);

        return new UserResDTO.UpdateProfileImageDTO(imageUrl);
    }
}
