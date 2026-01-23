package com.my4cut.domain.friend.dto.res;

import com.my4cut.domain.friend.entity.FriendRequest;
import com.my4cut.domain.friend.enums.FriendRequestStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendRequestResDto { //요청 조회용
    private Long requestId;
    private Long fromUserId;
    private String fromUserName;
    private FriendRequestStatus status;

    public record SendRequestResDto(
            FriendRequestStatus status
    ) {
        public static SendRequestResDto of(FriendRequest request) {
            return new SendRequestResDto(request.getStatus());
        }
    }

    public record ReceivedRequestResDto(
            Long reqId,
            SenderInfo sender
    ) {
        public static ReceivedRequestResDto of(FriendRequest request) {
            return new ReceivedRequestResDto(
                    request.getId(),
                    new SenderInfo(
                            request.getFromUser().getId(),
                            request.getFromUser().getNickname()
                    )
            );
        }

        public record SenderInfo(
                Long id,
                String nickname
        ) {}
    }

    public record AcceptRequestResDto(
            FriendRequestStatus status
    ) {
        public static AcceptRequestResDto of(FriendRequest request) {
            return new AcceptRequestResDto(request.getStatus());
        }
    }

    public record RejectRequestResDto(
            FriendRequestStatus status
    ) {
        public static RejectRequestResDto of(FriendRequest request) {
            return new RejectRequestResDto(request.getStatus());
        }
    }
}
