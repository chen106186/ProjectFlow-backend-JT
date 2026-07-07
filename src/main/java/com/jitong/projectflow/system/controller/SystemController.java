package com.jitong.projectflow.system.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.SystemUserResponse;
import com.jitong.projectflow.system.dto.UserCreateRequest;
import com.jitong.projectflow.system.dto.UserDetailResponse;
import com.jitong.projectflow.system.dto.UserEnabledUpdateRequest;
import com.jitong.projectflow.system.dto.UserPasswordResetRequest;
import com.jitong.projectflow.system.dto.UserRoleAssignRequest;
import com.jitong.projectflow.system.dto.UserUpdateRequest;
import com.jitong.projectflow.system.service.SystemUserManagementService;
import com.jitong.projectflow.system.service.SystemQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SystemQueryService systemQueryService;
    private final SystemUserManagementService systemUserManagementService;

    @GetMapping("/users")
    public ApiResponse<List<SystemUserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(systemQueryService.listUsers(keyword, departmentId, enabled), MDC.get("traceId"));
    }

    @PostMapping("/users")
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(systemUserManagementService.create(request), MDC.get("traceId"));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(systemUserManagementService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(systemUserManagementService.update(id, request), MDC.get("traceId"));
    }

    @PatchMapping("/users/{id}/enabled")
    public ApiResponse<UserDetailResponse> updateUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody UserEnabledUpdateRequest request) {
        return ApiResponse.success(systemUserManagementService.updateEnabled(id, request), MDC.get("traceId"));
    }

    @PatchMapping("/users/{id}/password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserPasswordResetRequest request) {
        systemUserManagementService.resetPassword(id, request);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<List<Long>> assignUserRoles(
            @PathVariable Long id,
            @RequestBody UserRoleAssignRequest request) {
        return ApiResponse.success(systemUserManagementService.assignRoles(id, request), MDC.get("traceId"));
    }

    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponse>> listDepartments() {
        return ApiResponse.success(systemQueryService.listDepartments(), MDC.get("traceId"));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(systemQueryService.listRoles(), MDC.get("traceId"));
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> listMenus() {
        return ApiResponse.success(systemQueryService.listMenus(), MDC.get("traceId"));
    }
}
