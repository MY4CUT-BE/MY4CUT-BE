package com.my4cut.global.init;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

//로그인 테스트용 파일
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Ensures a default test user exists by creating one when no users are present.
     *
     * If the user store is empty, inserts a test user with email "test@test.com",
     * nickname "테스트유저", friend code "TEST1234", and an encoded password.
     */
    @PostConstruct
    public void init() {
        // 이미 데이터가 있으면 아무 것도 안 함
        if (userRepository.count() > 0) return;

        User user = User.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("1234"))
                .nickname("테스트유저")
                .loginType(LoginType.EMAIL)
                .friendCode("TEST1234")
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
    }
}