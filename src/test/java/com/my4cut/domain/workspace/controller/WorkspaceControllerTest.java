package com.my4cut.domain.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.domain.workspace.dto.WorkspaceCreateRequestDto;
import com.my4cut.domain.workspace.dto.WorkspaceDeleteResponseDto;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
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
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                1L, "새 워크스페이스", 1L, LocalDateTime.now(), LocalDateTime.now(), false, 1, List.of(1L), List.of(), List.of());
        
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
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                1L, "내 워크스페이스", 1L, LocalDateTime.now(), LocalDateTime.now(), false, 1, List.of(1L), List.of(), List.of(2L));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspaceService.getMyWorkspaces(any())).willReturn(List.of(responseDto));

        // Act & Assert
        mockMvc.perform(get("/workspaces/me")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data[0].name").value("내 워크스페이스"))
                .andExpect(jsonPath("$.data[0].memberIds[0]").value(1L))
                .andExpect(jsonPath("$.data[0].pendingInvitationUserIds[0]").value(2L))
                .andExpect(jsonPath("$.data[0].alreadyInvitedFriendIds[0]").value(2L));
    }

    @Test
    @WithMockUser
    @DisplayName("워크스페이스 상세 조회 API 테스트: 이미 초대된 친구 ID 목록을 반환한다")
    void getWorkspaceInfo_Test() throws Exception {
        WorkspaceInfoResponseDto responseDto = new WorkspaceInfoResponseDto(
                1L,
                "조회용 워크스페이스",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                2,
                List.of(1L, 3L),
                List.of("https://example.com/owner.png", "https://example.com/member.png"),
                List.of(2L));

        given(workspaceService.getWorkspaceInfo(1L)).willReturn(responseDto);

        mockMvc.perform(get("/workspaces/{workspaceId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.name").value("조회용 워크스페이스"))
                .andExpect(jsonPath("$.data.memberIds[0]").value(1L))
                .andExpect(jsonPath("$.data.memberIds[1]").value(3L))
                .andExpect(jsonPath("$.data.pendingInvitationUserIds[0]").value(2L))
                .andExpect(jsonPath("$.data.alreadyInvitedFriendIds[0]").value(3L))
                .andExpect(jsonPath("$.data.alreadyInvitedFriendIds[1]").value(2L));
    }

    @Test
    @WithMockUser
    @DisplayName("워크스페이스 삭제 API 테스트")
    void deleteWorkspace_Test() throws Exception {
        WorkspaceDeleteResponseDto responseDto = new WorkspaceDeleteResponseDto(1L);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspaceService.deleteWorkspace(anyLong(), nullable(Long.class))).willReturn(responseDto);

        mockMvc.perform(delete("/workspaces/{workspaceId}", 1L)
                        .with(csrf())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data.ownerId").value(1L));
    }
}
