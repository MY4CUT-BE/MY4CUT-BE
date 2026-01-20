package com.my4cut.global.config;

import com.my4cut.domain.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configure and build the application's SecurityFilterChain.
     *
     * Disables CSRF, sets session management to stateless, permits unauthenticated access to
     * authentication and API documentation endpoints (/auth/login, /auth/signup, /auth/refresh,
     * /swagger-ui/**, /v3/api-docs/**), requires authentication for all other requests, and
     * registers the JWT authentication filter to run before UsernamePasswordAuthenticationFilter.
     *
     * @param http the HttpSecurity instance to configure
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // 인증 없이 허용 (화이트리스트)
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/auth/refresh",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 인증 필요(로그인 해야함)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Creates a BCryptPasswordEncoder for hashing and verifying user passwords.
     *
     * @return the BCryptPasswordEncoder instance used for encoding and matching passwords
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}