package com.my4cut.domain.auth.jwt;

import com.my4cut.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter jwtAuthenticationFilter =
            new JwtAuthenticationFilter(mock(JwtProvider.class), mock(UserRepository.class));

    @Test
    @DisplayName("비밀번호 재설정 경로는 JWT 필터를 건너뛴다")
    void shouldNotFilter_ResetPasswordPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/password/reset");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }
}
