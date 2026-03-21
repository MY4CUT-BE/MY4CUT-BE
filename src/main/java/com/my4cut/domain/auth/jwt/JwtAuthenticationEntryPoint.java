package com.my4cut.domain.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.global.response.ApiResponse;
import com.my4cut.global.response.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * Spring Security 미인증 응답을 프로젝트 공통 응답 형식으로 맞춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        log.debug("Unauthorized request: path={}, message={}", request.getRequestURI(), authException.getMessage());

        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.onFailure(ErrorCode.UNAUTHORIZED)));
    }
}
