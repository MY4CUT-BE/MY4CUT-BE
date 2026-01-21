package com.my4cut.domain.user.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 FCM 푸시 알림 토큰을 저장하는 엔티티이다.
 * 한 사용자가 여러 기기에서 로그인할 수 있으므로 다중 토큰을 관리한다.
 */
@Entity
@Table(name = "user_fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Builder
    public UserFcmToken(User user, String fcmToken, DeviceType deviceType) {
        this.user = user;
        this.fcmToken = fcmToken;
        this.deviceType = deviceType;
    }
}
