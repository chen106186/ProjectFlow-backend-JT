package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import com.jitong.projectflow.system.service.SystemQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtTokenService jwtTokenService;
    @MockBean
    CurrentUserPermissionService currentUserPermissionService;
    @MockBean
    SystemQueryService systemQueryService;

    @Test
    void protectedEndpointWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/system/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void openApiEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void systemUsersRequiresViewPermission() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void systemUsersAllowsViewPermission() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of("system:user:view"));
        when(systemQueryService.listUsers(any())).thenReturn(new PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void currentUserOnlyRequiresAuthentication() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());
        when(currentUserPermissionService.getCurrentUser()).thenReturn(CurrentUserProfileResponse.builder()
                .id(1L)
                .username("admin")
                .realName("Admin")
                .roles(List.of())
                .menus(List.of())
                .permissions(List.of())
                .build());

        mockMvc.perform(get("/api/system/me")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenService.createToken(userId, "tester");
    }
}
