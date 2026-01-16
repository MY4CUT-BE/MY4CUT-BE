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
            String profileImageUrl,
            String loginType
    ) {
        public static MeDTO from(User user) {
            return new MeDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getFriendCode(),
                    user.getProfileImageUrl(),
                    user.getLoginType().name()
            );
        }
    }
}
