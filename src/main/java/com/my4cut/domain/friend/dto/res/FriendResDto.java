package com.my4cut.domain.friend.dto.res;

import com.my4cut.domain.user.entity.User;
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
    public record SearchUserResDto(
            Long userId,
            String nickname,
            String profileImageUrl,
            boolean alreadyFriend,
            boolean outgoingRequest,
            boolean incomingRequest
    ) {
        public static SearchUserResDto of(
                User user,
                boolean alreadyFriend,
                boolean outgoingRequest,
                boolean incomingRequest
        ) {
            return new SearchUserResDto(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    alreadyFriend,
                    outgoingRequest,
                    incomingRequest
            );
        }
    }
}
