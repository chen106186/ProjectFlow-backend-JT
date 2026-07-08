package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.file.service.FileService;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import com.jitong.projectflow.system.service.SystemQueryService;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @MockBean
    TaskService taskService;
    @MockBean
    FileService fileService;

    @Test
    void protectedEndpointWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/system/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
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
                .andExpect(jsonPath("$.message").value("无权限访问该资源"));
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

    @Test
    void businessReadOnlyEndpointOnlyRequiresAuthentication() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());
        when(taskService.list(any())).thenReturn(new PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void taskUpdateRequiresUpdatePermission() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(put("/api/tasks/10")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void taskUpdateAllowsUpdatePermission() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of("task:update"));
        when(taskService.update(eq(10L), any())).thenReturn(TaskResponse.builder()
                .id(10L)
                .name("Design API")
                .status("TODO")
                .build());

        mockMvc.perform(put("/api/tasks/10")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void fileListOnlyRequiresAuthentication() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());
        when(fileService.list("TASK", 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/files")
                        .param("businessType", "TASK")
                        .param("businessId", "1")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void fileUploadRequiresUploadPermission() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(multipart("/api/files")
                        .file("file", "abc".getBytes())
                        .param("businessType", "TASK")
                        .param("businessId", "1")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void fileDeleteUsesServiceLevelBusinessAccess() throws Exception {
        when(currentUserPermissionService.getPermissionsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(delete("/api/files/10")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenService.createToken(userId, "tester");
    }
}
