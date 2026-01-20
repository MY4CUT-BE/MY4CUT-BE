package com.my4cut.domain.user.repository;

import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
 * Finds a user by their email address.
 *
 * @param email the email address to search for
 * @return an Optional containing the User with the specified email, or Optional.empty() if no user is found
 */
Optional<User> findByEmail(String email);

    /**
 * Checks whether a user with the specified email exists.
 *
 * @param email the email address to check for an existing user
 * @return `true` if a user with the specified email exists, `false` otherwise
 */
boolean existsByEmail(String email);
}