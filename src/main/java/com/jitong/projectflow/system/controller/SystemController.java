package com.jitong.projectflow.system.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.dto.DepartmentCreateRequest;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.DepartmentUpdateRequest;
import com.jitong.projectflow.system.dto.MenuCreateRequest;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.MenuUpdateRequest;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.dto.RoleCreateRequest;
import com.jitong.projectflow.system.dto.RoleDetailResponse;
import com.jitong.projectflow.system.dto.RoleMenuAssignRequest;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.RoleUpdateRequest;
import com.jitong.projectflow.system.dto.SystemUserQueryRequest;
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
import com.jitong.projectflow.system.service.SystemQueryService;
import com.jitong.projectflow.system.service.SystemUserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "当前用户、用户管理、部门管理、角色权限、菜单和操作日志接口")
public class SystemController {
    private final SystemQueryService systemQueryService;
    private final SystemUserManagementService systemUserManagementService;
    private final DepartmentManagementService departmentManagementService;
    private final RoleManagementService roleManagementService;
    private final MenuManagementService menuManagementService;
    private final CurrentUserPermissionService currentUserPermissionService;
    private final OperationLogQueryService operationLogQueryService;

    @Operation(summary = "查询当前用户信息",
            description = "查询当前登录用户的基础资料、角色、菜单和权限码。")
    @GetMapping("/me")
    public ApiResponse<CurrentUserProfileResponse> getCurrentUser() {
        return ApiResponse.success(currentUserPermissionService.getCurrentUser(), MDC.get("traceId"));
    }

    @Operation(summary = "查询当前用户权限码",
            description = "返回当前登录用户拥有的全部菜单和按钮权限码。")
    @GetMapping("/me/permissions")
    public ApiResponse<List<String>> getCurrentUserPermissions() {
        return ApiResponse.success(currentUserPermissionService.getCurrentUserPermissions(), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询操作日志",
            description = "按模块、业务类型、操作人和时间范围分页查询系统操作日志。")
    @GetMapping("/logs")
    public ApiResponse<PageResult<OperationLogResponse>> listOperationLogs(@Valid @ModelAttribute OperationLogQueryRequest request) {
        return ApiResponse.success(operationLogQueryService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询用户列表",
            description = "按账号、姓名、部门和启用状态分页查询系统用户。")
    @GetMapping("/users")
    public ApiResponse<PageResult<SystemUserResponse>> listUsers(@Valid @ModelAttribute SystemUserQueryRequest request) {
        return ApiResponse.success(systemQueryService.listUsers(request), MDC.get("traceId"));
    }

    @Operation(summary = "新增用户",
            description = "创建系统用户，设置所属部门、岗位、账号状态、初始密码和初始角色。")
    @PostMapping("/users")
    public ApiResponse<UserDetailResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(systemUserManagementService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询用户详情",
            description = "根据用户 ID 查询系统用户基础资料、部门和角色信息。")
    @GetMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(systemUserManagementService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑用户",
            description = "修改系统用户基础资料、所属部门和账号信息。")
    @PutMapping("/users/{id}")
    public ApiResponse<UserDetailResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(systemUserManagementService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "启用或禁用用户",
            description = "更新系统用户启用状态，禁用后该用户不可继续登录。")
    @PatchMapping("/users/{id}/enabled")
    public ApiResponse<UserDetailResponse> updateUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody UserEnabledUpdateRequest request) {
        return ApiResponse.success(systemUserManagementService.updateEnabled(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "重置用户密码",
            description = "为指定系统用户重置登录密码。")
    @PatchMapping("/users/{id}/password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserPasswordResetRequest request) {
        systemUserManagementService.resetPassword(id, request);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "分配用户角色",
            description = "为指定系统用户重新分配角色列表。")
    @PutMapping("/users/{id}/roles")
    public ApiResponse<List<Long>> assignUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignRequest request) {
        return ApiResponse.success(systemUserManagementService.assignRoles(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "查询部门树",
            description = "查询系统部门树形结构，用于用户归属和组织架构展示。")
    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponse>> listDepartments() {
        return ApiResponse.success(systemQueryService.listDepartments(), MDC.get("traceId"));
    }

    @Operation(summary = "新增部门",
            description = "新增部门节点并设置上级部门、排序和负责人信息。")
    @PostMapping("/departments")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        return ApiResponse.success(departmentManagementService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "编辑部门",
            description = "修改部门名称、上级部门、排序和负责人信息。")
    @PutMapping("/departments/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        return ApiResponse.success(departmentManagementService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除部门",
            description = "删除指定部门，存在子部门或关联用户时不允许删除。")
    @DeleteMapping("/departments/{id}")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        departmentManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "查询角色列表",
            description = "查询系统角色列表，用于权限分配和用户授权。")
    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(systemQueryService.listRoles(), MDC.get("traceId"));
    }

    @Operation(summary = "新增角色",
            description = "新增系统角色并设置角色编码、名称和启用状态。")
    @PostMapping("/roles")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleManagementService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询角色详情",
            description = "根据角色 ID 查询角色基础信息和已分配菜单权限。")
    @GetMapping("/roles/{id}")
    public ApiResponse<RoleDetailResponse> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleManagementService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑角色",
            description = "修改系统角色名称、编码、排序和启用状态。")
    @PutMapping("/roles/{id}")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleManagementService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除角色",
            description = "删除指定角色，已分配给用户时不允许删除。")
    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "查询角色菜单权限",
            description = "查询指定角色已分配的菜单和按钮权限 ID 列表。")
    @GetMapping("/roles/{id}/menus")
    public ApiResponse<List<Long>> getRoleMenus(@PathVariable Long id) {
        return ApiResponse.success(roleManagementService.getMenuIds(id), MDC.get("traceId"));
    }

    @Operation(summary = "分配角色菜单权限",
            description = "为指定角色重新分配菜单和按钮权限。")
    @PutMapping("/roles/{id}/menus")
    public ApiResponse<List<Long>> assignRoleMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuAssignRequest request) {
        return ApiResponse.success(roleManagementService.assignMenus(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "查询菜单列表",
            description = "查询系统菜单、目录和按钮权限列表。")
    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> listMenus() {
        return ApiResponse.success(systemQueryService.listMenus(), MDC.get("traceId"));
    }

    @Operation(summary = "新增菜单",
            description = "新增目录、菜单或按钮权限节点。")
    @PostMapping("/menus")
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.success(menuManagementService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "编辑菜单",
            description = "修改目录、菜单或按钮权限的名称、路径、权限码和排序。")
    @PutMapping("/menus/{id}")
    public ApiResponse<MenuResponse> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(menuManagementService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除菜单",
            description = "删除指定菜单或按钮权限节点，存在子节点时不允许删除。")
    @DeleteMapping("/menus/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        menuManagementService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
