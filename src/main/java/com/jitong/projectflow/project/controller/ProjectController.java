package com.jitong.projectflow.project.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.project.dto.GanttNodeResponse;
import com.jitong.projectflow.project.dto.GanttSummaryResponse;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.dto.ProjectResponse;
import com.jitong.projectflow.project.dto.ProjectStatsResponse;
import com.jitong.projectflow.project.dto.ProjectUpdateRequest;
import com.jitong.projectflow.project.service.GanttService;
import com.jitong.projectflow.project.service.ProjectService;
import com.jitong.projectflow.project.service.ProjectStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/projects")
@Tag(name = "项目管理", description = "管理类项目、执行类项目、项目详情和甘特图相关接口")
public class ProjectController {

    private final ProjectService projectService;
    private final GanttService ganttService;
    private final ProjectStatsService projectStatsService;

    public ProjectController(ProjectService projectService, GanttService ganttService, ProjectStatsService projectStatsService) {
        this.projectService = projectService;
        this.ganttService = ganttService;
        this.projectStatsService = projectStatsService;
    }

    @Operation(summary = "新建项目",
            description = "创建管理类或执行类项目，写入项目基础信息，并初始化项目状态。")
    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询项目列表",
            description = "按项目类型、状态、合同状态、项目经理和关键字查询项目清单。")
    @GetMapping
    public ApiResponse<PageResult<ProjectResponse>> listProjects(@Valid @ModelAttribute ProjectQueryRequest request) {
        return ApiResponse.success(projectService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "批量查询项目任务和 Bug 数量",
            description = "传入逗号分隔的项目 ID 列表，一次返回每个项目的任务数和 Bug 数，用于列表页展示，避免 N+1 请求。")
    @GetMapping("/stats")
    public ApiResponse<List<ProjectStatsResponse>> getStats(
            @org.springframework.web.bind.annotation.RequestParam String projectIds) {
        List<Long> ids = java.util.Arrays.stream(projectIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
        return ApiResponse.success(projectStatsService.batchStats(ids), MDC.get("traceId"));
    }

    @Operation(summary = "查询项目详情",
            description = "根据项目 ID 查询项目基础信息、状态、日期和负责人等详情。")
    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(projectService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑项目",
            description = "更新项目基础信息，系统会进行接口权限和业务数据归属校验。")

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.success(projectService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除项目",
            description = "逻辑删除指定项目，适用于项目误建或需要下线的场景。")

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "查询项目甘特图节点",
            description = "返回指定项目的甘特图节点列表，用于项目进度时间轴展示。")
    @GetMapping("/{projectId}/gantt")
    public ApiResponse<List<GanttNodeResponse>> getGanttNodes(@PathVariable Long projectId) {
        return ApiResponse.success(ganttService.getGanttNodes(projectId), MDC.get("traceId"));
    }

    @Operation(summary = "编辑项目甘特图节点",
            description = "更新项目节点的计划时间、实际时间、状态和进度等甘特图信息。")

    @PatchMapping("/{projectId}/nodes/{nodeId}")
    public ApiResponse<GanttNodeResponse> updateNode(
            @PathVariable Long projectId,
            @PathVariable Long nodeId,
            @RequestBody ProjectNodeUpdateRequest req) {
        return ApiResponse.success(ganttService.updateNode(projectId, nodeId, req), MDC.get("traceId"));
    }

    @Operation(summary = "查询项目甘特图统计",
            description = "统计指定项目的整体进度、延期任务和即将到期任务等摘要信息。")
    @GetMapping("/{projectId}/gantt/summary")
    public ApiResponse<GanttSummaryResponse> getGanttSummary(@PathVariable Long projectId) {
        return ApiResponse.success(ganttService.getSummary(projectId), MDC.get("traceId"));
    }
}
