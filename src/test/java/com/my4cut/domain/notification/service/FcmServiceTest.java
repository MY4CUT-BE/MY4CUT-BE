package com.my4cut.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock private FcmClient fcmClient;

    private FcmService fcmService;

    @BeforeEach
    void setUp() {
        fcmService = new FcmService(fcmClient);
        ReflectionTestUtils.setField(fcmService, "firebaseEnabled", true);
    }

    @Test
    void sendPush_returnsSuccessWhenFirebaseAcceptsMessage() throws Exception {
        given(fcmClient.send(any(Message.class))).willReturn("message-id");

        FcmSendResult result = sendPush("valid-token");

        assertThat(result).isEqualTo(FcmSendResult.SUCCESS);
    }

    @Test
    void sendPush_returnsInvalidTokenWhenTokenIsUnregistered() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
        given(fcmClient.send(any(Message.class))).willThrow(exception);

        FcmSendResult result = sendPush("expired-token");

        assertThat(result).isEqualTo(FcmSendResult.INVALID_TOKEN);
    }

    @Test
    void sendPush_doesNotPropagateUnexpectedClientFailure() throws Exception {
        given(fcmClient.send(any(Message.class))).willThrow(new IllegalStateException("temporary failure"));

        FcmSendResult result = sendPush("valid-token");

        assertThat(result).isEqualTo(FcmSendResult.FAILED);
    }

    @Test
    void sendPush_skipsClientWhenFirebaseIsDisabled() {
        ReflectionTestUtils.setField(fcmService, "firebaseEnabled", false);

        FcmSendResult result = sendPush("valid-token");

        assertThat(result).isEqualTo(FcmSendResult.SKIPPED);
        verifyNoInteractions(fcmClient);
    }

    private FcmSendResult sendPush(String token) {
        return fcmService.sendPush(
                token,
                "title",
                "body",
                "MEDIA_COMMENT",
                1L,
                2L,
                3L,
                4L
        );
    }
}
