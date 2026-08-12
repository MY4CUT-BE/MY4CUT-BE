package com.my4cut.domain.user.repository;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.LoginType;
import com.my4cut.domain.user.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndStatusNot(String email, UserStatus status);

    //friendCode로 User 조회가 필요해 메서드 추가
    Optional<User> findByFriendCode(String friendCode);

    Optional<User> findByLoginTypeAndOauthId(LoginType loginType, String oauthId);

}
