package com.my4cut.domain.workspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.domain.workspace.dto.WorkspacePhotoResponseDto;
import com.my4cut.domain.workspace.dto.WorkspacePhotoUploadRequestDto;
import com.my4cut.domain.workspace.service.WorkspacePhotoService;
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

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = WorkspacePhotoController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, com.my4cut.domain.auth.jwt.JwtAuthenticationFilter.class}))
class WorkspacePhotoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private WorkspacePhotoService workspacePhotoService;
    @MockBean private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;
    @MockBean private com.my4cut.domain.user.repository.UserRepository userRepository;
    @MockBean private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser
    @DisplayName("사진 업로드 API 테스트")
    void uploadPhotos_Test() throws Exception {
        // Arrange
        Long workspaceId = 1L;
        WorkspacePhotoUploadRequestDto requestDto = new WorkspacePhotoUploadRequestDto(List.of(10L));
        WorkspacePhotoResponseDto responseDto = new WorkspacePhotoResponseDto(10L, "url", "presigned", null, null, true, LocalDateTime.now(), "닉네임");
        
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspacePhotoService.uploadPhotos(any(), any(), any())).willReturn(List.of(responseDto));

        // Act & Assert
        mockMvc.perform(post("/workspaces/{workspaceId}/photos", workspaceId)
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data[0].fileKey").value("url"));
    }

    @Test
    @WithMockUser
    @DisplayName("사진 목록 조회 API 테스트")
    void getPhotos_Test() throws Exception {
        // Arrange
        Long workspaceId = 1L;
        WorkspacePhotoResponseDto responseDto = new WorkspacePhotoResponseDto(10L, "url", "presigned", null, null, true, LocalDateTime.now(), "닉네임");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        given(workspacePhotoService.getPhotos(any(), any(), any())).willReturn(List.of(responseDto));

        // Act & Assert
        mockMvc.perform(get("/workspaces/{workspaceId}/photos", workspaceId)
                        .param("sort", "latest")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data[0].mediaId").value(10L));
    }
}
