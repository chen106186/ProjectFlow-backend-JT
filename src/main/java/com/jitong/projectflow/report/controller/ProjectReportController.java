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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class ProjectReportController {
    private final ProjectReportService projectReportService;

    @PostMapping
    @PreAuthorize("hasAuthority('project-report:create')")
    public ApiResponse<ProjectReportResponse> create(@Valid @RequestBody ProjectReportCreateRequest request) {
        return ApiResponse.success(projectReportService.create(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectReportResponse>> list(@Valid @ModelAttribute ProjectReportQueryRequest request) {
        return ApiResponse.success(projectReportService.list(request), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectReportResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(projectReportService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<ProjectReportResponse> update(@PathVariable Long id, @Valid @RequestBody ProjectReportUpdateRequest request) {
        return ApiResponse.success(projectReportService.update(id, request), MDC.get("traceId"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<ProjectReportResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ProjectReportStatusUpdateRequest request) {
        return ApiResponse.success(projectReportService.updateStatus(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectReportService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<ProjectReportItemResponse> createItem(@PathVariable Long id, @Valid @RequestBody ProjectReportItemCreateRequest request) {
        return ApiResponse.success(projectReportService.createItem(id, request), MDC.get("traceId"));
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<ProjectReportItemResponse> updateItem(@PathVariable Long id,
                                                              @PathVariable Long itemId,
                                                              @Valid @RequestBody ProjectReportItemUpdateRequest request) {
        return ApiResponse.success(projectReportService.updateItem(id, itemId, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('project-report:update')")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        projectReportService.deleteItem(id, itemId);
        return ApiResponse.success(null, MDC.get("traceId"));
    }
}
