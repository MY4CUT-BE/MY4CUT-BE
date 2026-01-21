package com.my4cut.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /**
     * 인증이 필요 없는 경로는 필터 자체를 타지 않도록 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/auth/login")
                || path.startsWith("/auth/signup")
                || path.startsWith("/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Authorization 헤더 자체가 없으면 → 인증 실패 (보호 API 기준)
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            unauthorized(response, "인증 토큰이 없습니다.");
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtProvider.validateAccessToken(token);
            Long userId = Long.valueOf(claims.getSubject());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            unauthorized(response, "유효하지 않은 토큰입니다.");
        }
    }

    /**
     * 401 Unauthorized JSON 응답
     */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {
              "code": "C401",
              "message": "%s",
              "data": null
            }
        """.formatted(message));
    }
}
