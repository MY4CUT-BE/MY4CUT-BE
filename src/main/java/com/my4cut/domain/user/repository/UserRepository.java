package com.my4cut.domain.user.repository;

import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    //friendCode로 User 조회가 필요해 메서드 추가
    Optional<User> findByFriendCode(String friendCode);
}
