package com.jitong.projectflow.requirement.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.requirement.dto.RequirementCreateRequest;
import com.jitong.projectflow.requirement.dto.RequirementLogResponse;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.requirement.dto.RequirementResponse;
import com.jitong.projectflow.requirement.dto.RequirementStatusUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementUpdateRequest;
import com.jitong.projectflow.requirement.service.RequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/requirements")
@Tag(name = "需求管理", description = "需求提交、查询、编辑和状态流转接口")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @Operation(summary = "提交需求",
            description = "创建项目需求，默认进入待评审状态，并记录提交人和操作日志。")
    @PreAuthorize("hasAuthority('requirement:create')")
    @PostMapping
    public ApiResponse<RequirementResponse> create(@Valid @RequestBody RequirementCreateRequest req) {
        return ApiResponse.success(requirementService.create(req), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询需求列表",
            description = "按项目等条件分页查询需求数据，用于需求管理列表。")
    @PreAuthorize("hasAuthority('requirement')")
    @GetMapping
    public ApiResponse<PageResult<RequirementResponse>> list(@Valid @ModelAttribute RequirementQueryRequest request) {
        return ApiResponse.success(requirementService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询我的需求",
            description = "查询当前登录用户创建的全部需求。")
    @GetMapping("/my")
    public ApiResponse<List<RequirementResponse>> listMine() {
        return ApiResponse.success(requirementService.listMine(), MDC.get("traceId"));
    }

    @Operation(summary = "查询需求详情",
            description = "根据需求 ID 查询标题、类型、优先级、状态、描述和标签。")
    @GetMapping("/{id}")
    public ApiResponse<RequirementResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(requirementService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑需求",
            description = "更新需求基础信息，适用于需求创建人或具备权限的人员维护需求。")
    @PreAuthorize("hasAuthority('requirement:update')")
    @PutMapping("/{id}")
    public ApiResponse<RequirementResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody RequirementUpdateRequest req) {
        return ApiResponse.success(requirementService.update(id, req), MDC.get("traceId"));
    }

    @Operation(summary = "查询需求操作日志",
            description = "按需求 ID 查询该需求的全部操作历史记录。")
    @GetMapping("/{id}/logs")
    public ApiResponse<List<RequirementLogResponse>> listLogs(@PathVariable Long id) {
        return ApiResponse.success(requirementService.listLogs(id), MDC.get("traceId"));
    }

    @Operation(summary = "更新需求状态",
            description = "按需求状态机更新需求状态，例如待评审、已采纳或已拒绝。")
    @PreAuthorize("hasAuthority('requirement:update')")
    @PatchMapping("/{id}/status")
    public ApiResponse<RequirementResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody RequirementStatusUpdateRequest req) {
        return ApiResponse.success(requirementService.updateStatus(id, req), MDC.get("traceId"));
    }
}
