package com.my4cut.domain.friend.event;

public record FriendAcceptedEvent(
        Long requesterId,
        Long accepterId
) {
}
