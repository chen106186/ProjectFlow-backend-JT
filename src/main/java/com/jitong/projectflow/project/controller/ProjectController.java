package com.jitong.projectflow.project.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.project.dto.GanttNodeResponse;
import com.jitong.projectflow.project.dto.GanttSummaryResponse;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.dto.ProjectResponse;
import com.jitong.projectflow.project.dto.ProjectUpdateRequest;
import com.jitong.projectflow.project.service.GanttService;
import com.jitong.projectflow.project.service.ProjectService;
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
public class ProjectController {

    private final ProjectService projectService;
    private final GanttService ganttService;

    public ProjectController(ProjectService projectService, GanttService ganttService) {
        this.projectService = projectService;
        this.ganttService = ganttService;
    }

    @PostMapping
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.create(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectResponse>> listProjects(@Valid @ModelAttribute ProjectQueryRequest request) {
        return ApiResponse.success(projectService.list(request), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(projectService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectResponse> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.success(projectService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @GetMapping("/{projectId}/gantt")
    public ApiResponse<List<GanttNodeResponse>> getGanttNodes(@PathVariable Long projectId) {
        return ApiResponse.success(ganttService.getGanttNodes(projectId), MDC.get("traceId"));
    }

    @PatchMapping("/{projectId}/nodes/{nodeId}")
    public ApiResponse<GanttNodeResponse> updateNode(
            @PathVariable Long projectId,
            @PathVariable Long nodeId,
            @RequestBody ProjectNodeUpdateRequest req) {
        return ApiResponse.success(ganttService.updateNode(projectId, nodeId, req), MDC.get("traceId"));
    }

    @GetMapping("/{projectId}/gantt/summary")
    public ApiResponse<GanttSummaryResponse> getGanttSummary(@PathVariable Long projectId) {
        return ApiResponse.success(ganttService.getSummary(projectId), MDC.get("traceId"));
    }
}
