package com.my4cut.domain.notification.dto;

import com.my4cut.domain.notification.dto.req.NotificationReqDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationReqDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerToken_rejectsBlankToken() {
        var request = new NotificationReqDto.RegisterTokenDto(" ", "ANDROID");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("fcmToken");
    }

    @Test
    void registerToken_rejectsUnsupportedDevice() {
        var request = new NotificationReqDto.RegisterTokenDto("token", "WEB");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("device");
    }

    @Test
    void registerToken_acceptsDeviceIgnoringCase() {
        var request = new NotificationReqDto.RegisterTokenDto("token", "android");

        assertThat(validator.validate(request)).isEmpty();
    }
}
