package com.my4cut.domain.notification.service;

import com.my4cut.domain.notification.dto.req.NotificationReqDto;
import com.my4cut.domain.notification.dto.res.NotificationResDto;
import com.my4cut.domain.notification.entity.Notification;
import com.my4cut.domain.notification.repository.NotificationRepository;
import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import com.my4cut.domain.user.repository.UserFcmTokenRepository;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // FCM 토큰 등록
    @Transactional
    public NotificationResDto.RegisterTokenResDto registerFcmToken(
            Long userId,
            NotificationReqDto.RegisterTokenDto request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        DeviceType deviceType = DeviceType.valueOf(request.device().toUpperCase());

        UserFcmToken existingToken = userFcmTokenRepository
                .findByUserAndDeviceType(user, deviceType)
                .orElse(null);

        if (existingToken != null) {
            existingToken.updateToken(request.fcmToken());
            return NotificationResDto.RegisterTokenResDto.of(existingToken.getId());
        }

        UserFcmToken fcmToken = UserFcmToken.builder()
                .user(user)
                .fcmToken(request.fcmToken())
                .deviceType(deviceType)
                .build();

        UserFcmToken savedToken = userFcmTokenRepository.save(fcmToken);

        return NotificationResDto.RegisterTokenResDto.of(savedToken.getId());
    }

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDto.NotificationItemDto> getNotifications(
            Long userId,
            int page
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, 20); // 페이지당 20개

        Page<Notification> notifications = notificationRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);

        return notifications.stream()
                .map(NotificationResDto.NotificationItemDto::of)
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public NotificationResDto.ReadNotificationResDto markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 읽음 처리
        notification.markAsRead();

        return NotificationResDto.ReadNotificationResDto.of(notification);
    }
}