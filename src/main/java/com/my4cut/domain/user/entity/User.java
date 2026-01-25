package com.my4cut.domain.user.entity;

import com.my4cut.domain.common.BaseEntity;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서비스 사용자 정보를 저장하는 엔티티이다.
 * 이메일, 닉네임, 프로필 이미지, 로그인 방식, 친구 코드 등 사용자의 기본 정보를 관리한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * EMAIL 로그인 시 사용
     * KAKAO 로그인 사용자는 null 가능
     */
    @Column(unique = true)
    private String email;

    /**
     * EMAIL 로그인 시 사용
     * 소셜 로그인은 null
     */
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    /**
     * KAKAO 로그인 고유 식별자
     * EMAIL 로그인은 null
     */
    @Column(name = "oauth_id", unique = true)
    private String oauthId;

    @Column(name = "friend_code", nullable = false, unique = true)
    private String friendCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(
            String email,
            String password,
            String nickname,
            String profileImageUrl,
            LoginType loginType,
            String oauthId,
            String friendCode,
            UserStatus status
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl =
                profileImageUrl != null
                        ? profileImageUrl
                        : com.my4cut.global.image.ImageConstants.DEFAULT_PROFILE_IMAGE_URL;
        this.loginType = loginType;
        this.oauthId = oauthId;
        this.friendCode = friendCode;
        this.status = status;
    }

    public void withdraw() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
