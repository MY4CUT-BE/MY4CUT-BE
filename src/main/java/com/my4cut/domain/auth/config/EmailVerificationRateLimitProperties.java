package com.my4cut.domain.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 운영 환경에서 이메일 인증 요청 제한값을 조정할 수 있도록 제공하는 설정이다.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "email-verification.rate-limit")
public class EmailVerificationRateLimitProperties {

    @Valid
    private Limit sendClient = new Limit(20L, Duration.ofMinutes(10));
    @Valid
    private Limit sendEmail = new Limit(5L, Duration.ofHours(1));
    @Valid
    private Limit verifyClient = new Limit(60L, Duration.ofMinutes(10));

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Limit {
        @Min(1)
        private long maxAttempts;
        @NotNull
        private Duration window;

        @AssertTrue(message = "요청 제한 시간은 0보다 커야 합니다.")
        public boolean isWindowPositive() {
            return window != null && !window.isZero() && !window.isNegative();
        }
    }
}
