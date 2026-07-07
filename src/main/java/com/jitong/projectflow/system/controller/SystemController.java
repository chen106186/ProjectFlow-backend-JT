package com.jitong.projectflow.system.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.system.dto.DepartmentResponse;
import com.jitong.projectflow.system.dto.MenuResponse;
import com.jitong.projectflow.system.dto.RoleResponse;
import com.jitong.projectflow.system.dto.SystemUserResponse;
import com.jitong.projectflow.system.service.SystemQueryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    private final SystemQueryService systemQueryService;

    @GetMapping("/users")
    public ApiResponse<List<SystemUserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(systemQueryService.listUsers(keyword, departmentId, enabled), MDC.get("traceId"));
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
