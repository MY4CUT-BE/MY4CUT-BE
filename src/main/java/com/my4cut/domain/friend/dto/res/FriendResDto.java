package com.my4cut.domain.friend.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendResDto { // 친구목록
    private Long friendId;
    private Long userId;
    private String nickname;
    private Boolean isFavorite;

    public record FavoriteFriendResDto(
            boolean isFavorite
    ) {
        public static FavoriteFriendResDto of(boolean isFavorite) {
            return new FavoriteFriendResDto(isFavorite);
        }
    }
}
