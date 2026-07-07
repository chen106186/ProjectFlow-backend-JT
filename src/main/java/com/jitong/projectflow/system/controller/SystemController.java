package com.jitong.projectflow.system.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.DepartmentCreateRequest;
import com.jitong.projectflow.system.dto.DepartmentUpdateRequest;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.MenuCreateRequest;
import com.jitong.projectflow.system.dto.MenuUpdateRequest;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.RoleCreateRequest;
import com.jitong.projectflow.system.dto.RoleDetailResponse;
import com.jitong.projectflow.system.dto.RoleMenuAssignRequest;
import com.jitong.projectflow.system.dto.RoleUpdateRequest;
import com.jitong.projectflow.system.dto.SystemUserResponse;
import com.jitong.projectflow.system.dto.UserCreateRequest;
import com.jitong.projectflow.system.dto.UserDetailResponse;
import com.jitong.projectflow.system.dto.UserEnabledUpdateRequest;
import com.jitong.projectflow.system.dto.UserPasswordResetRequest;
import com.jitong.projectflow.system.dto.UserRoleAssignRequest;
import com.jitong.projectflow.system.dto.UserUpdateRequest;
import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import com.jitong.projectflow.system.service.DepartmentManagementService;
import com.jitong.projectflow.system.service.MenuManagementService;
import com.jitong.projectflow.system.service.OperationLogQueryService;
import com.jitong.projectflow.system.service.RoleManagementService;
import com.jitong.projectflow.system.service.SystemUserManagementService;
import com.jitong.projectflow.system.service.SystemQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    private final DepartmentManagementService departmentManagementService;
    private final RoleManagementService roleManagementService;
    private final MenuManagementService menuManagementService;
    private final CurrentUserPermissionService currentUserPermissionService;
    private final OperationLogQueryService operationLogQueryService;

    @GetMapping("/me")
    public ApiResponse<CurrentUserProfileResponse> getCurrentUser() {
        return ApiResponse.success(currentUserPermissionService.getCurrentUser(), MDC.get("traceId"));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<List<String>> getCurrentUserPermissions() {
        return ApiResponse.success(currentUserPermissionService.getCurrentUserPermissions(), MDC.get("traceId"));
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLogResponse>> listOperationLogs(@ModelAttribute OperationLogQueryRequest request) {
        return ApiResponse.success(operationLogQueryService.list(request), MDC.get("traceId"));
    }

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

    @PostMapping("/departments")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        return ApiResponse.success(departmentManagementService.create(request), MDC.get("traceId"));
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @RequestBody DepartmentUpdateRequest request) {
        return ApiResponse.success(departmentManagementService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        departmentManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(systemQueryService.listRoles(), MDC.get("traceId"));
    }

    @PostMapping("/roles")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleManagementService.create(request), MDC.get("traceId"));
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<RoleDetailResponse> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleManagementService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleManagementService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @GetMapping("/roles/{id}/menus")
    public ApiResponse<List<Long>> getRoleMenus(@PathVariable Long id) {
        return ApiResponse.success(roleManagementService.getMenuIds(id), MDC.get("traceId"));
    }

    @PutMapping("/roles/{id}/menus")
    public ApiResponse<List<Long>> assignRoleMenus(@PathVariable Long id, @RequestBody RoleMenuAssignRequest request) {
        return ApiResponse.success(roleManagementService.assignMenus(id, request), MDC.get("traceId"));
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> listMenus() {
        return ApiResponse.success(systemQueryService.listMenus(), MDC.get("traceId"));
    }

    @PostMapping("/menus")
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.success(menuManagementService.create(request), MDC.get("traceId"));
    }

    @PutMapping("/menus/{id}")
    public ApiResponse<MenuResponse> updateMenu(@PathVariable Long id, @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(menuManagementService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/menus/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        menuManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
