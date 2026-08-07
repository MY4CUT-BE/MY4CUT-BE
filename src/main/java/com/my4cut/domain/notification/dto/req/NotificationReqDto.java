package com.my4cut.domain.notification.dto.req;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record NotificationReqDto() {

    public record RegisterTokenDto(
            @NotBlank
            String fcmToken,
            @NotBlank
            @Pattern(regexp = "(?i)ANDROID|IOS")
            String device  // ANDROID or IOS
    ) {}

    public record MarkReadByIdsDto(
            List<Long> notificationIds
    ) {}
}
