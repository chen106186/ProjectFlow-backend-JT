package com.jitong.projectflow.dashboard.controller;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import com.jitong.projectflow.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(new DashboardSummaryResponse(0, 0, 0, 0), MDC.get("traceId"));
    }

    @GetMapping("/todos")
    public ApiResponse<List<TodoItemResponse>> todos() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }

    @GetMapping("/my-statistics")
    public ApiResponse<MyStatisticsResponse> myStatistics() {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(dashboardService.getMyStatistics(userId), MDC.get("traceId"));
    }
}
