package com.my4cut.domain.notification.controller;

import com.my4cut.domain.notification.dto.req.NotificationReqDto;
import com.my4cut.domain.notification.dto.res.NotificationResDto;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // FCM 토큰 등록
    @PostMapping("/token")
    public ApiResponse<NotificationResDto.RegisterTokenResDto> registerToken(
            @AuthenticationPrincipal Long userId,
            @RequestBody NotificationReqDto.RegisterTokenDto request
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                notificationService.registerFcmToken(userId, request)
        );
    }

    // 알림 목록 조회
    @GetMapping
    public ApiResponse<List<NotificationResDto.NotificationItemDto>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                notificationService.getNotifications(userId, page)
        );
    }

    // 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResDto.ReadNotificationResDto> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                notificationService.markAsRead(userId, id)
        );
    }
}