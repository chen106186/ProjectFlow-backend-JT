package com.jitong.projectflow.export.controller;

import com.jitong.projectflow.export.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExcelExportService exportService;

    public ExportController(ExcelExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/operation-logs")
    public void exportOperationLogs(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"operation-logs.xlsx\"");
        exportService.exportOperationLogs(response.getOutputStream());
    }

    @GetMapping("/tasks")
    public void exportTasks(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"tasks.xlsx\"");
        exportService.exportTasks(response.getOutputStream());
    }

    @GetMapping("/requirements")
    public void exportRequirements(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"requirements.xlsx\"");
        exportService.exportRequirements(response.getOutputStream());
    }

    @GetMapping("/bugs")
    public void exportBugs(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"bugs.xlsx\"");
        exportService.exportBugs(response.getOutputStream());
    }

    @GetMapping("/projects/{projectId}/gantt")
    public void exportGantt(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"gantt.xlsx\"");
        exportService.exportGantt(projectId, response.getOutputStream());
    }
}
