package com.my4cut.global.config;

import com.my4cut.domain.auth.jwt.JwtAuthenticationFilter;
import com.my4cut.domain.auth.jwt.JwtAuthenticationEntryPoint;
import com.my4cut.global.security.AdminAuthorizationManager;
import com.my4cut.global.security.RestAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AdminAuthorizationManager adminAuthorizationManager;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/password/reset").permitAll()

                        // 인증 없이 허용 (화이트리스트)
                        .requestMatchers(
                                "/auth/login",
                                "/auth/kakao",
                                "/auth/check-email",
                                "/auth/signup",
                                "/auth/refresh",
                                "/auth/email/**",
                                "/admin-ui",
                                "/admin-ui/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 관리자 전용 API
                        .requestMatchers("/admin/**").access(adminAuthorizationManager)

                        // 인증 필요
                        .requestMatchers(
                                "/auth/withdraw",
                                "/users/**",
                                "/friends/**",  // 친구 API
                                "/notifications/**", //FCM 토큰 API
                                "/poses/**",    // 포즈 API
                                "/media/**",    // 미디어 API
                                "/day4cut/**",  // 하루네컷 API
                                "/workspaces/**", // 워크스페이스 API
                                "/albums/**",    // 앨범 API
                                "/api/v1/**",
                                "/images/**"

                        ).authenticated()

                        // 그 외 전부 차단
                        .anyRequest().denyAll()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
