package com.jitong.projectflow.task.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @GetMapping
    public ApiResponse<List<String>> listTasks() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
