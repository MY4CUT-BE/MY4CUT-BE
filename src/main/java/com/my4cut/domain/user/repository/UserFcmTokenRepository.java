package com.my4cut.domain.user.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
// FCM 토큰 등록 API 개발을 위한 Repository 생성
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByUserAndDeviceType(User user, DeviceType deviceType);
}