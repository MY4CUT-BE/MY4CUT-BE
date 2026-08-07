package com.my4cut.domain.notification.controller;

import com.my4cut.domain.notification.service.NotificationService;
import com.my4cut.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerToken_rejectsBlankToken() throws Exception {
        mockMvc.perform(post("/notifications/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcmToken":" ","device":"ANDROID"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void registerToken_rejectsUnsupportedDevice() throws Exception {
        mockMvc.perform(post("/notifications/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fcmToken":"token","device":"WEB"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }
}
