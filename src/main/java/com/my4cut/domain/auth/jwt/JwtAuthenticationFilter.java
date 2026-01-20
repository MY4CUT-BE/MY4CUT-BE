package com.my4cut.domain.auth.jwt;

import com.my4cut.domain.user.entity.User;
import com.my4cut.domain.user.enums.UserStatus;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * Authenticates the incoming HTTP request using a Bearer JWT from the Authorization header
     * and, when valid, installs a UsernamePasswordAuthenticationToken in the SecurityContext.
     *
     * If a Bearer token is present but invalid, missing, expired, or the referenced user is not active,
     * the security context is cleared, the response status is set to 401 (Unauthorized), and request
     * processing is halted for this filter.
     *
     * @param request     the incoming servlet request
     * @param response    the servlet response used to set an unauthorized status on failure
     * @param filterChain the remaining filter chain to continue processing when authentication succeeds or no token is present
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                // 토큰 검증 + Claims 추출
                Claims claims = jwtProvider.validateAccessToken(token);
                Long userId = Long.valueOf(claims.getSubject());

                // 사용자 조회
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

                // 탈퇴 유저 차단
                if (user.getStatus() != UserStatus.ACTIVE) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }

                // 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getId(),          // principal
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // 토큰이 있는데 인증 실패 → 바로 차단
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}