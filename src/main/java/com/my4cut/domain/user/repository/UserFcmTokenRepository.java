package com.my4cut.domain.user.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.entity.UserFcmToken;
import com.my4cut.domain.user.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
// FCM 토큰 등록 API 개발을 위한 Repository 생성
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    Optional<UserFcmToken> findByUserAndDeviceType(User user, DeviceType deviceType);
    List<UserFcmToken> findAllByUser(User user);

    @Modifying
    @Query("delete from UserFcmToken token where token.user = :user")
    int deleteAllByUser(@Param("user") User user);
}
