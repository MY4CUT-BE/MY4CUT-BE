package com.my4cut.domain.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.domain.workspace.dto.WorkspaceInvitationResponseDto;
import com.my4cut.domain.workspace.dto.WorkspaceInviteRequestDto;
import com.my4cut.domain.workspace.enums.InvitationStatus;
import com.my4cut.domain.workspace.service.WorkspaceInvitationService;
import com.my4cut.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceInvitationController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, com.my4cut.domain.auth.jwt.JwtAuthenticationFilter.class}))
class WorkspaceInvitationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WorkspaceInvitationService workspaceInvitationService;
    @MockBean private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;
    @MockBean private com.my4cut.domain.user.repository.UserRepository userRepository;
    @MockBean private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser
    @DisplayName("멤버 초대 API 테스트")
    void inviteMembers_Test() throws Exception {
        // Arrange
        WorkspaceInviteRequestDto requestDto = new WorkspaceInviteRequestDto(1L, List.of(2L));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());

        // Act & Assert
        mockMvc.perform(post("/workspaces/invitations")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("내 초대 목록 조회 API 테스트")
    void getMyInvitations_Test() throws Exception {
        // Arrange
        WorkspaceInvitationResponseDto responseDto = new WorkspaceInvitationResponseDto(100L, "워크스페이스", "초대자", InvitationStatus.PENDING, LocalDateTime.now());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspaceInvitationService.getMyInvitations(any())).willReturn(List.of(responseDto));

        // Act & Assert
        mockMvc.perform(get("/workspaces/invitations/me")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data[0].workspaceName").value("워크스페이스"));
    }
}
