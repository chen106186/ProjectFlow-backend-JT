package com.jitong.projectflow.export.controller;

import com.jitong.projectflow.export.service.ExcelExportService;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/exports")
@Tag(name = "Excel 导出", description = "任务、需求、Bug、甘特图和操作日志导出接口")
public class ExportController {

    private final ExcelExportService exportService;

    public ExportController(ExcelExportService exportService) {
        this.exportService = exportService;
    }

    @Operation(summary = "导出操作日志",
            description = "导出系统操作日志为 Excel 文件。")
    @GetMapping("/operation-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('system:log:view')")
    public void exportOperationLogs(@Valid OperationLogQueryRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"operation-logs.xlsx\"");
        exportService.exportOperationLogs(request, response.getOutputStream());
    }

    @Operation(summary = "导出任务列表",
            description = "导出任务数据为 Excel 文件。")
    @GetMapping("/tasks")
    public void exportTasks(@Valid TaskQueryRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"tasks.xlsx\"");
        exportService.exportTasks(request, response.getOutputStream());
    }

    @Operation(summary = "导出需求列表",
            description = "导出需求数据为 Excel 文件。")
    @GetMapping("/requirements")
    public void exportRequirements(@Valid RequirementQueryRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"requirements.xlsx\"");
        exportService.exportRequirements(request, response.getOutputStream());
    }

    @Operation(summary = "导出 Bug 列表",
            description = "导出 Bug 数据为 Excel 文件。")
    @GetMapping("/bugs")
    public void exportBugs(@Valid BugQueryRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"bugs.xlsx\"");
        exportService.exportBugs(request, response.getOutputStream());
    }

    @Operation(summary = "导出项目甘特图",
            description = "根据项目 ID 导出该项目甘特图节点数据为 Excel 文件。")
    @GetMapping("/projects/{projectId}/gantt")
    public void exportGantt(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"gantt.xlsx\"");
        exportService.exportGantt(projectId, response.getOutputStream());
    }
}
