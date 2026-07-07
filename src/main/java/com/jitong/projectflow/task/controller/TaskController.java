package com.jitong.projectflow.task.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.dto.TaskUpdateRequest;
import com.jitong.projectflow.task.service.TaskService;
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

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    public ApiResponse<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.success(taskService.create(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks(@ModelAttribute TaskQueryRequest request) {
        return ApiResponse.success(taskService.list(request), MDC.get("traceId"));
    }

    @GetMapping("/my")
    public ApiResponse<List<TaskResponse>> listMine() {
        return ApiResponse.success(taskService.listMine(), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(taskService.getById(id), MDC.get("traceId"));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> update(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        return ApiResponse.success(taskService.update(id, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @PatchMapping("/{id}/actual-time")
    public ApiResponse<TaskResponse> updateActualTime(@PathVariable Long id, @RequestBody TaskActualTimeUpdateRequest request) {
        return ApiResponse.success(taskService.updateActualTime(id, request), MDC.get("traceId"));
    }
}
