package com.jitong.projectflow.dashboard.controller;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import com.jitong.projectflow.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "首页看板", description = "首页统计、待办事项和我的统计接口")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "查询首页项目统计",
            description = "返回管理类项目、执行类项目、进行中项目和已完成项目统计。")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(dashboardService.getSummary(), MDC.get("traceId"));
    }

    @Operation(summary = "查询首页待办列表",
            description = "返回首页待办事项列表，包含任务、缺陷等提醒数据。")
    @GetMapping("/todos")
    public ApiResponse<List<TodoItemResponse>> todos() {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(dashboardService.listTodos(userId), MDC.get("traceId"));
    }

    @Operation(summary = "查询我的统计",
            description = "统计当前登录用户的任务、Bug、需求和未读通知数量。period 支持 today/week/month/year/all（默认 all）。")
    @GetMapping("/my-statistics")
    public ApiResponse<MyStatisticsResponse> myStatistics(
            @RequestParam(defaultValue = "all") String period) {
        Long userId = CurrentUserContext.userId();
        return ApiResponse.success(dashboardService.getMyStatistics(userId, period), MDC.get("traceId"));
    }
}
