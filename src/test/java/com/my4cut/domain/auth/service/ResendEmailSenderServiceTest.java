package com.my4cut.domain.auth.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.config.ResendConfig;
import com.my4cut.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderServiceTest {

    private MockRestServiceServer server;
    private ResendEmailSenderService resendEmailSenderService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = new ResendConfig()
                .applyResendDefaults(RestClient.builder(), "test_resend_api_key");
        server = MockRestServiceServer.bindTo(builder).build();
        resendEmailSenderService = new ResendEmailSenderService(builder.build());
        ReflectionTestUtils.setField(resendEmailSenderService, "fromEmail", "MY4CUT <noreply@example.com>");
    }

    @Test
    @DisplayName("Resend 발송 성공: 2xx 응답을 성공으로 처리한다")
    void sendVerificationCode_Success_2xx() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test_resend_api_key"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.from").value("MY4CUT <noreply@example.com>"))
                .andExpect(jsonPath("$.to[0]").value("test@example.com"))
                .andExpect(jsonPath("$.subject").value("[MY4CUT] 이메일 인증코드"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("123456")))
                .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        resendEmailSenderService.sendVerificationCode("test@example.com", "123456");

        server.verify();
    }

    @Test
    @DisplayName("Resend 발송 실패: 4xx 응답을 기존 이메일 발송 실패 예외로 변환한다")
    void sendVerificationCode_Fail_4xx() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"validation_error\",\"message\":\"Invalid request\"}"));

        assertThatThrownBy(() -> resendEmailSenderService.sendVerificationCode("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_SEND_FAILED);

        server.verify();
    }

    @Test
    @DisplayName("Resend 발송 실패: 5xx 응답을 기존 이메일 발송 실패 예외로 변환한다")
    void sendVerificationCode_Fail_5xx() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"internal_server_error\",\"message\":\"Unexpected error\"}"));

        assertThatThrownBy(() -> resendEmailSenderService.sendVerificationCode("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_SEND_FAILED);

        server.verify();
    }

    @Test
    @DisplayName("Resend 발송 실패: timeout/network 예외를 발송 결과 불명 예외로 변환한다")
    void sendVerificationCode_Fail_Timeout() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> resendEmailSenderService.sendVerificationCode("test@example.com", "123456"))
                .isInstanceOf(EmailDeliveryUnknownException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_DELIVERY_UNKNOWN);

        server.verify();
    }
}
