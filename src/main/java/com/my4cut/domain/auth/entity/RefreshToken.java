package com.my4cut.domain.auth.entity;

import com.my4cut.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 500)
    private String token;

    /**
     * Create a RefreshToken for the given user with the specified token value.
     *
     * @param user  the user to which this refresh token belongs
     * @param token the token string to store (persisted with a maximum length of 500 characters)
     */
    public RefreshToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    /**
     * Replaces the stored refresh token with the provided value.
     *
     * @param token the new refresh token value to store
     */
    public void updateToken(String token) {
        this.token = token;
    }
}