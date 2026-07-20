package com.my4cut.domain.pose.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my4cut.domain.pose.dto.req.PoseCreateRequest;
import com.my4cut.domain.pose.service.PoseAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminPoseController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
class AdminPoseControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PoseAdminService poseAdminService;
    @MockBean
    private com.my4cut.domain.auth.jwt.JwtProvider jwtProvider;
    @MockBean
    private com.my4cut.domain.user.repository.UserRepository userRepository;
    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createsPoseFromMultipartRequest() throws Exception {
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new PoseCreateRequest("포즈", 2))
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "pose.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}
        );

        mockMvc.perform(multipart("/admin/poses")
                        .file(metadata)
                        .file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("C2011"));

        verify(poseAdminService).createPose(any(PoseCreateRequest.class), eq(image));
    }

    @Test
    void rejectsCreateRequestWithoutImage() throws Exception {
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new PoseCreateRequest("포즈", 2))
        );

        mockMvc.perform(multipart("/admin/poses").file(metadata))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C4001"));
    }
}
