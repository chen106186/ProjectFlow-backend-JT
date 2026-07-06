package com.jitong.projectflow.dashboard.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(new DashboardSummaryResponse(0, 0, 0, 0), MDC.get("traceId"));
    }

    @GetMapping("/todos")
    public ApiResponse<List<TodoItemResponse>> todos() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
