package com.jitong.projectflow.project.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.project.dto.GanttNodeResponse;
import com.jitong.projectflow.project.dto.GanttSummaryResponse;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.service.GanttService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final GanttService ganttService;

    public ProjectController(GanttService ganttService) {
        this.ganttService = ganttService;
    }

    @GetMapping
    public ApiResponse<List<String>> listProjects() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
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
