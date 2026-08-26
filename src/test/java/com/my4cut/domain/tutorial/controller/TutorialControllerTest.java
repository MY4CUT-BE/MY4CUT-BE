package com.my4cut.domain.tutorial.controller;

import com.my4cut.domain.auth.jwt.JwtAuthenticationFilter;
import com.my4cut.domain.auth.jwt.JwtProvider;
import com.my4cut.domain.tutorial.dto.TutorialStatusResponseDto;
import com.my4cut.domain.tutorial.enums.TutorialType;
import com.my4cut.domain.tutorial.service.TutorialService;
import com.my4cut.domain.user.repository.UserRepository;
import com.my4cut.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TutorialController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
class TutorialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TutorialService tutorialService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getStatus_returnsEveryTutorialStatus() throws Exception {
        Long userId = 1L;
        TutorialStatusResponseDto response = TutorialStatusResponseDto.from(
                EnumSet.of(TutorialType.HOME)
        );
        given(tutorialService.getStatus(nullable(Long.class))).willReturn(response);

        mockMvc.perform(get("/tutorials")
                        .with(authentication(authToken(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorials.length()")
                        .value(TutorialType.values().length))
                .andExpect(jsonPath("$.data.tutorials[0].type").value("HOME"))
                .andExpect(jsonPath("$.data.tutorials[0].completed").value(true))
                .andExpect(jsonPath("$.data.tutorials[1].type").value("UPLOAD_DATE"))
                .andExpect(jsonPath("$.data.tutorials[1].completed").value(false));
    }

    @Test
    void complete_acceptsCaseInsensitiveTutorialTypeAndReturnsEveryStatus() throws Exception {
        Long userId = 1L;
        TutorialStatusResponseDto response = TutorialStatusResponseDto.from(
                EnumSet.of(TutorialType.UPLOAD_DATE)
        );
        given(tutorialService.complete(nullable(Long.class), eq(TutorialType.UPLOAD_DATE)))
                .willReturn(response);

        mockMvc.perform(patch("/tutorials/{tutorialType}/complete", "upload_date")
                        .with(csrf())
                        .with(authentication(authToken(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorials.length()")
                        .value(TutorialType.values().length))
                .andExpect(jsonPath("$.data.tutorials[1].type").value("UPLOAD_DATE"))
                .andExpect(jsonPath("$.data.tutorials[1].completed").value(true));
    }

    private UsernamePasswordAuthenticationToken authToken(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }
}
