package com.jitong.projectflow.task.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskBatchCreateRequest;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.dto.TaskRiskStatisticsResponse;
import com.jitong.projectflow.task.dto.TaskUpdateRequest;
import com.jitong.projectflow.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务创建、查询、编辑、我的任务和实际时间填报接口")
public class TaskController {
    private final TaskService taskService;

    @Operation(summary = "新建任务",
            description = "创建项目任务，系统根据计划时间和实际时间自动计算任务状态。")
    @PostMapping
    public ApiResponse<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(request), MDC.get("traceId"));
    }

    @Operation(summary = "批量创建任务",
            description = "一次提交多条任务，支持父任务 ID 和排序号，用于执行类项目创建任务表格。")
    @PostMapping("/batch")
    public ApiResponse<List<TaskResponse>> batchCreate(@Valid @RequestBody TaskBatchCreateRequest request) {
        return ApiResponse.success(taskService.batchCreate(request), MDC.get("traceId"));
    }

    @Operation(summary = "下载任务导入模板",
            description = "下载任务 Excel 导入模板，前端可用于批量创建任务。")
    @GetMapping("/import-template")
    public ResponseEntity<InputStreamResource> downloadImportTemplate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        taskService.writeImportTemplate(out);
        byte[] bytes = out.toByteArray();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''task-import-template.xlsx")
                .body(new InputStreamResource(new ByteArrayInputStream(bytes)));
    }

    @Operation(summary = "导入任务 Excel",
            description = "读取任务导入模板中的任务数据并批量创建任务。")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<TaskResponse>> importTasks(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(taskService.importTasks(file), MDC.get("traceId"));
    }

    @Operation(summary = "分页查询任务列表",
            description = "按项目、负责人、优先级、状态、计划截止日期和关键字查询全部任务。")
    @GetMapping
    public ApiResponse<PageResult<TaskResponse>> listTasks(@Valid @ModelAttribute TaskQueryRequest request) {
        return ApiResponse.success(taskService.list(request), MDC.get("traceId"));
    }

    @Operation(summary = "查询我的任务",
            description = "查询当前登录用户作为执行人的全部任务。")
    @GetMapping("/my")
    public ApiResponse<List<TaskResponse>> listMine() {
        return ApiResponse.success(taskService.listMine(), MDC.get("traceId"));
    }

    @Operation(summary = "任务风险统计",
            description = "统计各状态任务数量，用于风险看板。projectId 不传则统计全部项目。")
    @GetMapping("/risk-statistics")
    public ApiResponse<TaskRiskStatisticsResponse> riskStatistics(
            @RequestParam(required = false) Long projectId) {
        return ApiResponse.success(taskService.getRiskStatistics(projectId), MDC.get("traceId"));
    }

    @Operation(summary = "查询任务详情",
            description = "根据任务 ID 查询任务基础信息、计划时间、实际时间、状态和备注。")
    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(taskService.getById(id), MDC.get("traceId"));
    }

    @Operation(summary = "编辑任务",
            description = "更新任务基础信息，适用于项目负责人或具备权限的人员维护任务。")

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return ApiResponse.success(taskService.update(id, request), MDC.get("traceId"));
    }

    @Operation(summary = "删除任务",
            description = "逻辑删除指定任务及其所有子孙任务，系统会校验接口权限和业务数据归属。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "查询子任务列表",
            description = "查询指定任务的直接子任务列表，按排序号升序返回。")
    @GetMapping("/{id}/subtasks")
    public ApiResponse<List<TaskResponse>> listSubtasks(@PathVariable Long id) {
        return ApiResponse.success(taskService.listSubtasks(id), MDC.get("traceId"));
    }

    @Operation(summary = "填报任务实际时间",
            description = "执行人填写实际开始、实际结束和备注，系统自动刷新任务状态并记录操作日志。")
    @PatchMapping("/{id}/actual-time")
    public ApiResponse<TaskResponse> updateActualTime(@PathVariable Long id, @Valid @RequestBody TaskActualTimeUpdateRequest request) {
        return ApiResponse.success(taskService.updateActualTime(id, request), MDC.get("traceId"));
    }
}
