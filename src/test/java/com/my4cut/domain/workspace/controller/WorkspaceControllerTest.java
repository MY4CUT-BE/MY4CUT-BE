package com.my4cut.domain.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceInfoResponseDto;
import com.my4cut.domain.workspace.service.WorkspaceService;
import com.my4cut.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = WorkspaceController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, com.my4cut.domain.auth.jwt.JwtAuthenticationFilter.class}))
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WorkspaceService workspaceService;
    @MockBean private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;
    @MockBean private com.my4cut.domain.user.repository.UserRepository userRepository;
    @MockBean private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser
    @DisplayName("워크스페이스 생성 API 테스트")
    void createWorkspace_Test() throws Exception {
        // Arrange
        WorkspaceCreateRequestDto requestDto = new WorkspaceCreateRequestDto("새 워크스페이스");
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(1L, "새 워크스페이스", 1L, LocalDateTime.now(), LocalDateTime.now());
        
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspaceService.createWorkspace(any(), any())).willReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/workspaces")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.name").value("새 워크스페이스"));
    }

    @Test
    @WithMockUser
    @DisplayName("내 워크스페이스 목록 조회 API 테스트")
    void getMyWorkspaces_Test() throws Exception {
        // Arrange
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(1L, "내 워크스페이스", 1L, LocalDateTime.now(), LocalDateTime.now());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspaceService.getMyWorkspaces(any())).willReturn(List.of(responseDto));

        // Act & Assert
        mockMvc.perform(get("/workspaces/me")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data[0].name").value("내 워크스페이스"));
    }
}
