package com.my4cut.domain.auth.repository;

import com.my4cut.domain.auth.entity.RefreshToken;
import com.my4cut.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
 * Retrieves the refresh token associated with the given user.
 *
 * @param user the user whose refresh token is being queried
 * @return an {@code Optional} containing the user's {@link RefreshToken} if present, otherwise {@code Optional.empty()}
 */
Optional<RefreshToken> findByUser(User user);

    /**
 * Finds a refresh token entity by its token string.
 *
 * @param token the refresh token string to look up
 * @return an Optional containing the RefreshToken if found, otherwise Optional.empty()
 */
Optional<RefreshToken> findByToken(String token);

    /**
 * Deletes all RefreshToken entities associated with the specified user.
 *
 * @param user the user whose refresh token(s) should be removed
 */
void deleteByUser(User user);
}