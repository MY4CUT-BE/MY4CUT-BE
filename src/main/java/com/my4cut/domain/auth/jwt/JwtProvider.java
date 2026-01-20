package com.my4cut.domain.auth.jwt;

import com.my4cut.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    private static final long ACCESS_TOKEN_EXPIRE = 1000L * 60 * 30; // 30분
    private static final long REFRESH_TOKEN_EXPIRE = 1000L * 60 * 60 * 24 * 14; /**
     * Derives a SecretKey for HS256 signing from the configured secret string.
     *
     * @return a SecretKey suitable for HMAC-SHA (HS256) signing
     */

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a JWT access token for the given user.
     *
     * The token's subject is the user's ID and it contains a `loginType` claim
     * set to the user's login type name. The token is signed with the provider's
     * HMAC-SHA key and has the configured access-token expiration.
     *
     * @param user the user whose ID becomes the token subject and whose login type is stored in the `loginType` claim
     * @return the signed JWT access token string with the configured access-token expiration
     */
    public String createAccessToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("loginType", user.getLoginType().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Creates a refresh JWT for the given user with the user's ID as the token subject and a 14-day expiration.
     *
     * @param user the user for whom to create the refresh token
     * @return the serialized JWT refresh token string
     */
    public String createRefreshToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates and parses the given JWT access token and returns its claims.
     *
     * @param token the JWT access token string to verify and parse
     * @return the token's claims payload
     */
    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}