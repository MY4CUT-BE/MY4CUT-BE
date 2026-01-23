package com.my4cut.domain.notification.dto.req;

public record NotificationReqDto() {

    public record RegisterTokenDto(
            String fcmToken,
            String device  // ANDROID or IOS
    ) {}
}