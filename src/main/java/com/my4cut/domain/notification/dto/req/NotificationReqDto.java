package com.my4cut.domain.notification.dto.req;
import java.util.List;

public record NotificationReqDto() {

    public record RegisterTokenDto(
            String fcmToken,
            String device  // ANDROID or IOS
    ) {}

    public record MarkReadByIdsDto(
            List<Long> notificationIds
    ) {}
}