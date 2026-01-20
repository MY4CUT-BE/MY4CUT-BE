package com.my4cut.domain.user.service;

import com.my4cut.domain.user.dto.UserReqDTO;
import com.my4cut.domain.user.dto.UserResDTO;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Fetches profile information for the specified user.
     *
     * @param userId the identifier of the user to retrieve
     * @return a {@link UserResDTO.MeDTO} representing the user's profile
     * @throws BusinessException if no user exists with the given ID (ErrorCode.NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public UserResDTO.MeDTO getMyInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return UserResDTO.MeDTO.from(user);
    }

    /**
     * Update the nickname of the specified user.
     *
     * @param userId the identifier of the user whose nickname will be updated
     * @param request DTO containing the new nickname
     * @return a DTO containing the user's updated nickname
     * @throws BusinessException if the user does not exist or if the user's status is DELETED (ErrorCode.UNAUTHORIZED)
     */
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
}