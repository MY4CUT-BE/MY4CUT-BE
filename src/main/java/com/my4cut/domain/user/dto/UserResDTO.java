package com.my4cut.domain.user.dto;

import com.my4cut.domain.user.entity.User;
import lombok.Builder;

public class UserResDTO {

    // 로그인 응답
    @Builder
    public record LoginDTO(
            Long userId,
            String accessToken,
            String refreshToken
    ) {}

    // 내 정보 조회
    public record MeDTO(
            Long userId,
            String email,
            String nickname,
            String friendCode,
            String profileImageFileKey,
            String profileImageViewUrl,
            String loginType,

            long thisMonthDay4CutCount
    ) {
        public static MeDTO from(User user, String profileImageViewUrl, long thisMonthDay4CutCount) {
            return new MeDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getFriendCode(),
                    user.getProfileImageUrl(),
                    profileImageViewUrl,
                    user.getLoginType().name(),
                    thisMonthDay4CutCount
            );
        }
    }

    // 닉네임 변경
    public record UpdateNicknameDTO(
            String updatedNickname
    ) {}

    // 프로필 이미지 변경
    public record UpdateProfileImageDTO(
            String fileKey,
            String viewUrl
    ) {}
}
