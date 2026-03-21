package com.my4cut.domain.auth.service;

import com.my4cut.global.exception.BusinessException;
import com.my4cut.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/*
 * AWS SES를 통해 인증 메일을 발송하는 구현체다.
 */
@Service
@RequiredArgsConstructor
public class SesEmailSenderService implements EmailSenderService {

    private static final String SUBJECT = "[MY4CUT] 이메일 인증코드";

    private final SesV2Client sesV2Client;

    @Value("${ses.from-email}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        String body = """
                안녕하세요.

                MY4CUT 이메일 인증코드는 아래와 같습니다.

                %s

                5분 이내에 입력해 주세요.
                """.formatted(code);

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromEmail)
                .destination(Destination.builder()
                        .toAddresses(toEmail)
                        .build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder()
                                        .data(SUBJECT)
                                        .charset("UTF-8")
                                        .build())
                                .body(Body.builder()
                                        .text(Content.builder()
                                                .data(body)
                                                .charset("UTF-8")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            sesV2Client.sendEmail(request);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_SEND_FAILED, exception);
        }
    }
}
