package com.my4cut.domain.notification.controller;

import com.my4cut.domain.notification.dto.req.NotificationReqDto;
import com.my4cut.domain.notification.dto.res.NotificationResDto;
import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification", description = "알림 토큰 및 내역 관리 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // FCM 토큰 등록
    @Operation(
            summary = "FCM 토큰 등록",
            description = "사용자의 FCM 토큰을 서버에 등록합니다. 푸시 알림 전송을 위해 로그인 이후 또는 토큰 갱신 시 호출해야 합니다"
    )
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
    @Operation(
            summary = "알림 목록 조회",
            description = "사용자가 수신한 알림 목록을 최신순으로 조회합니다."
    )
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
    @Operation(
            summary = "알림 읽음 처리",
            description = "사용자가 선택한 알림을 읽음 상태로 변경합니다."
    )
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