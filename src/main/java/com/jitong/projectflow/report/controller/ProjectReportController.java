package com.jitong.projectflow.report.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.report.dto.ProjectReportCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportItemCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportItemResponse;
import com.jitong.projectflow.report.dto.ProjectReportItemUpdateRequest;
import com.jitong.projectflow.report.dto.ProjectReportQueryRequest;
import com.jitong.projectflow.report.dto.ProjectReportResponse;
import com.jitong.projectflow.report.dto.ProjectReportStatusUpdateRequest;
import com.jitong.projectflow.report.dto.ProjectReportUpdateRequest;
import com.jitong.projectflow.report.service.ProjectReportService;
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

@RestController
@RequestMapping("/api/project-reports")
@RequiredArgsConstructor
@Tag(name = "项目汇报", description = "项目汇报、汇报状态和准备工作管理接口")
public class ProjectReportController {
    private final ProjectReportService projectReportService;

    @Operation(summary = "新建项目汇报",
            description = "为指定项目创建阶段性汇报，记录汇报主题、内容、日期和准备事项。")
    @PostMapping

    public ApiResponse<ProjectReportResponse> create(@Valid @RequestBody ProjectReportCreateRequest request) {
        return ApiResponse.success(projectReportService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询项目汇报",
            description = "按项目、状态、汇报日期范围和关键字分页查询项目汇报。")
    @GetMapping
    public ApiResponse<PageResult<ProjectReportResponse>> list(@Valid @ModelAttribute ProjectReportQueryRequest request) {
        return ApiResponse.success(projectReportService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询项目汇报详情",
            description = "根据汇报 ID 查询项目汇报基础信息和准备工作明细。")
    @GetMapping("/{id}")
    public ApiResponse<ProjectReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(projectReportService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑项目汇报",
            description = "修改项目汇报主题、内容、日期、状态和备注信息。")
    @PutMapping("/{id}")

    public ApiResponse<ProjectReportResponse> update(@PathVariable Long id, @Valid @RequestBody ProjectReportUpdateRequest request) {
        return ApiResponse.success(projectReportService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "更新项目汇报状态",
            description = "单独更新项目汇报状态，用于草稿、已提交、已完成等状态流转。")
    @PatchMapping("/{id}/status")

    public ApiResponse<ProjectReportResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ProjectReportStatusUpdateRequest request) {
        return ApiResponse.success(projectReportService.updateStatus(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除项目汇报",
            description = "逻辑删除指定项目汇报及其关联准备工作。")
    @DeleteMapping("/{id}")

    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectReportService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "新增汇报准备工作",
            description = "为指定项目汇报新增准备工作条目。")
    @PostMapping("/{id}/items")

    public ApiResponse<ProjectReportItemResponse> createItem(@PathVariable Long id, @Valid @RequestBody ProjectReportItemCreateRequest request) {
        return ApiResponse.success(projectReportService.createItem(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "编辑汇报准备工作",
            description = "修改指定项目汇报下的准备工作内容、负责人或完成状态。")
    @PutMapping("/{id}/items/{itemId}")

    public ApiResponse<ProjectReportItemResponse> updateItem(@PathVariable Long id,
                                                              @PathVariable Long itemId,
                                                              @Valid @RequestBody ProjectReportItemUpdateRequest request) {
        return ApiResponse.success(projectReportService.updateItem(id, itemId, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除汇报准备工作",
            description = "删除指定项目汇报下的一条准备工作。")
    @DeleteMapping("/{id}/items/{itemId}")

    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        projectReportService.deleteItem(id, itemId);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
