package com.my4cut.global.security;

import com.my4cut.domain.pose.service.PoseAdminService;
import com.my4cut.domain.image.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "admin.user-ids=1,42")
@AutoConfigureMockMvc
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PoseAdminService poseAdminService;
    @MockBean
    private ImageStorageService imageStorageService;

    @Test
    void unauthenticatedAdminRequestReturns401() throws Exception {
        mockMvc.perform(delete("/admin/poses/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C4011"));
    }

    @Test
    void regularUserAdminRequestReturns403() throws Exception {
        mockMvc.perform(delete("/admin/poses/10")
                        .with(authentication(authToken(2L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C4031"));
    }

    @Test
    void configuredAdminCanAccessAdminRequest() throws Exception {
        mockMvc.perform(delete("/admin/poses/10")
                        .with(authentication(authToken(1L))))
                .andExpect(status().isOk());

        verify(poseAdminService).deletePose(10L);
    }

    @Test
    void adminUiIsAccessibleBeforeLogin() throws Exception {
        mockMvc.perform(get("/admin-ui"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin-ui/index.html"))
                .andExpect(status().isOk());
    }

    private UsernamePasswordAuthenticationToken authToken(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }
}
