package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.file.service.FileService;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import com.jitong.projectflow.system.service.SystemQueryService;
import com.jitong.projectflow.system.service.SystemUserManagementService;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

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
    @MockBean
    SystemUserManagementService systemUserManagementService;

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
    void authenticatedDirectoryRemainsAvailableForBusinessSelectors() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        when(systemQueryService.listUsers(any())).thenReturn(new PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void systemUsersAllowsViewPermission() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("system:user:view"));
        when(systemQueryService.listUsers(any())).thenReturn(new PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/system/users")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void currentUserOnlyRequiresAuthentication() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
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
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        when(taskService.list(any())).thenReturn(new PageResult<>(0, 1, 20, List.of()));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void taskUpdateHonorsBusinessAccessDenial() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        when(taskService.update(eq(10L), any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据"));

        mockMvc.perform(put("/api/tasks/10")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void taskUpdateAllowsUpdatePermission() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("task:update"));
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
                .andExpect(jsonPath("$.data.id").value("10"));
    }

    @Test
    void fileListOnlyRequiresAuthentication() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        when(fileService.list("TASK", 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/files")
                        .param("businessType", "TASK")
                        .param("businessId", "1")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void fileUploadHonorsBusinessAccessDenial() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        when(fileService.upload(eq("TASK"), eq(1L), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据"));

        mockMvc.perform(multipart("/api/files")
                        .file("file", "abc".getBytes())
                        .param("businessType", "TASK")
                        .param("businessId", "1")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void batchDownloadChecksAccessBeforeSendingZip() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据"))
                .when(fileService).requireBatchDownloadAccess(eq(List.of(10L)), isNull());

        mockMvc.perform(post("/api/files/batch-download").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fileIds\":[10]}"))
                .andExpect(status().isForbidden());
        verify(fileService).requireBatchDownloadAccess(List.of(10L), null);
    }

    @Test
    void batchDownloadWorkerKeepsAuthenticatedIdentity() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("ROLE_ADMIN"));
        AtomicReference<Long> streamedAs = new AtomicReference<>();
        AtomicReference<Boolean> administrator = new AtomicReference<>();
        doAnswer(invocation -> {
            streamedAs.set(CurrentUserContext.userIdOrNull());
            administrator.set(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                    .stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
            try (var zip = new java.util.zip.ZipOutputStream(invocation.getArgument(2, OutputStream.class))) {
                zip.putNextEntry(new ZipEntry("proof.txt"));
                zip.write("ok".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return null;
        }).when(fileService).batchDownload(eq(List.of(10L)), isNull(), any(OutputStream.class));

        byte[] response = mockMvc.perform(post("/api/files/batch-download").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fileIds\":[10]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(streamedAs.get()).isEqualTo(1L);
        assertThat(administrator.get()).isTrue();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("proof.txt");
        }
    }

    @Test
    void rejectedJwtDoesNotLeaveIdentityOnPublicInlineRoute() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "当前用户已被禁用"));
        AtomicReference<Long> inlineUser = new AtomicReference<>();
        when(fileService.inline(10L)).thenAnswer(invocation -> {
            inlineUser.set(CurrentUserContext.userIdOrNull());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        });

        mockMvc.perform(get("/api/files/10/inline").header("Authorization", bearerToken(1L)))
                .andExpect(status().isUnauthorized());
        assertThat(inlineUser.get()).isNull();
    }

    @Test
    void ordinaryUserCannotResetPasswordsAssignRolesOrReadAuditLogs() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("system:user:update"));
        mockMvc.perform(patch("/api/system/users/20/password").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"long-password\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/system/users/20/roles").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[10]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/system/logs").header("Authorization", bearerToken(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPermissionCannotAssignPrivilegedInitialRoles() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("system:user:create"));
        mockMvc.perform(post("/api/system/users").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\",\"realName\":\"New User\",\"password\":\"long-password\",\"roleIds\":[10]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delegatedRoleAndMenuPermissionsCannotGrantNewAuthorities() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of(
                "system:role:create", "system:role:update", "system:role:assign-menu", "system:menu:update",
                "system:user:assign-role", "system:user:reset-password", "system:department:update"));
        mockMvc.perform(post("/api/system/roles").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"SUPER_ADMIN\",\"name\":\"Admin\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/system/roles/10/menus").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"menuIds\":[10]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/system/menus/10").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"system:log:view\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/system/departments/10").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"总经办\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/system/users/20/password").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"long-password\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/system/users/20/roles").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[10]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanResetPasswordsAndAssignRoles() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of("ROLE_ADMIN"));
        when(systemUserManagementService.assignRoles(eq(20L), any())).thenReturn(List.of(10L));
        mockMvc.perform(patch("/api/system/users/20/password").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"long-password\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/system/users/20/roles").header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleIds\":[10]}"))
                .andExpect(status().isOk());
        verify(systemUserManagementService).resetPassword(eq(20L), any());
        verify(systemUserManagementService).assignRoles(eq(20L), any());
    }

    @Test
    void fileDeleteUsesServiceLevelBusinessAccess() throws Exception {
        when(currentUserPermissionService.getAuthoritiesByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(delete("/api/files/10")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenService.createToken(userId, "tester");
    }
}
