package com.my4cut.domain.auth;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    // 임시 사용자 저장소
    private static final Map<String, User> STORE = new HashMap<>();

    static {
        // 테스트 계정
        // email: test@my4cut.com
        // password: 1234
        STORE.put(
                "test@my4cut.com",
                new User(1L, "test@my4cut.com",
                        "$2a$10$sLWmGhA.zKMYPG4ijalTq.mv2PWgfMj7DaZudOKMtQsQwKCKB87la")
        );
        // 위 비밀번호는 BCrypt로 인코딩된 "1234"
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(STORE.get(email));
    }
}