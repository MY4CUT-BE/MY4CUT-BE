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

    // 알림 개별 삭제
    @Operation(
            summary = "알림 개별 삭제",
            description = "사용자가 선택한 알림을 삭제합니다."
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(userId, id); // void타입
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

    // 알림 전체 삭제
    @Operation(
            summary = "알림 전체 삭제",
            description = "사용자의 모든 알림을 삭제합니다."
    )
    @DeleteMapping
    public ApiResponse<Void> deleteAllNotifications(
            @AuthenticationPrincipal Long userId
    ) {
        notificationService.deleteAllNotifications(userId);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

    // 페이지 단위 읽음 처리
    @Operation(
            summary = "알림 페이지 읽음 처리",
            description = "조회한 페이지의 알림을 읽음 상태로 변경합니다."
    )
    @PatchMapping("/read-page")
    public ApiResponse<Void> markPageAsRead(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        notificationService.markPageAsRead(userId, page);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }

    // 읽지 않은 알림 여부 조회
    @Operation(
            summary = "읽지 않은 알림 여부 조회",
            description = "읽지 않은 알림이 있으면 true를 반환합니다."
    )
    @GetMapping("/unread-status")
    public ApiResponse<NotificationResDto.UnreadStatusResDto> getUnreadStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                notificationService.getUnreadStatus(userId)
        );
    }
}