package com.my4cut.domain.auth.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailSenderService implements EmailSenderService {

    private static final String SUBJECT = "[MY4CUT] 이메일 인증코드";

    private final RestClient resendRestClient;

    @Value("${mail.from}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        ResendEmailRequest request = new ResendEmailRequest(
                fromEmail,
                List.of(toEmail),
                SUBJECT,
                buildTextBody(code)
        );

        try {
            resendRestClient.post()
                    .uri("/emails")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            log.error(
                    "Failed to send verification email. email={}, provider=RESEND, failureType={}, status={}, statusText={}",
                    maskEmail(toEmail),
                    classifyFailure(statusCode),
                    statusCode.value(),
                    exception.getStatusText(),
                    exception
            );
            throw new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED, exception);
        } catch (ResourceAccessException exception) {
            log.error(
                    "Verification email delivery result is unknown. email={}, provider=RESEND, failureType=DELIVERY_UNKNOWN, exceptionType={}, message={}",
                    maskEmail(toEmail),
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );
            throw new EmailDeliveryUnknownException(exception);
        } catch (RestClientException exception) {
            log.error(
                    "Failed to send verification email. email={}, provider=RESEND, failureType=UNKNOWN, exceptionType={}, message={}",
                    maskEmail(toEmail),
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );
            throw new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED, exception);
        }
    }

    private String buildTextBody(String code) {
        return """
                안녕하세요.

                MY4CUT 이메일 인증코드는 아래와 같습니다.

                %s

                5분 이내에 입력해 주세요.
                """.formatted(code);
    }

    private String classifyFailure(HttpStatusCode statusCode) {
        if (statusCode.value() == 429 || statusCode.is5xxServerError()) {
            return "TRANSIENT";
        }

        return "PERMANENT";
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        return localPart.charAt(0) + "***@" + domain.charAt(0) + "***";
    }

    private record ResendEmailRequest(
            String from,
            List<String> to,
            String subject,
            String text
    ) {
    }
}
