package com.my4cut.domain.notification;

import com.my4cut.domain.notification.enums.NotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {

    @ParameterizedTest
    @MethodSource("figmaNotificationMessages")
    void formatsTheFiveFigmaNotificationMessages(
            NotificationType type,
            String expected
    ) {
        assertThat(NotificationMessage.format(type, "화운", "test"))
                .isEqualTo(expected);
    }

    private static Stream<Arguments> figmaNotificationMessages() {
        return Stream.of(
                Arguments.of(
                        NotificationType.WORKSPACE_INVITE,
                        "화운님이 test 스페이스에 회원님을 초대했습니다."
                ),
                Arguments.of(
                        NotificationType.FRIEND_REQUEST,
                        "화운님이 회원님에게 친구 요청을 보냈습니다."
                ),
                Arguments.of(
                        NotificationType.MEDIA_COMMENT,
                        "화운님이 test 스페이스에 댓글을 남겼습니다."
                ),
                Arguments.of(
                        NotificationType.MEDIA_UPLOADED,
                        "화운님이 test 스페이스에 사진을 업로드했습니다."
                ),
                Arguments.of(
                        NotificationType.FRIEND_ACCEPTED,
                        "화운님이 친구 초대를 수락하였습니다."
                )
        );
    }
}
