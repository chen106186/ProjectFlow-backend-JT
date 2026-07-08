package com.jitong.projectflow.taskcalendar.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarDayResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarMonthResponse;
import com.jitong.projectflow.taskcalendar.service.TaskCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/task-calendar")
@RequiredArgsConstructor
@Tag(name = "任务日历", description = "任务月历和日历详情查询接口")
public class TaskCalendarController {
    private final TaskCalendarService taskCalendarService;

    @Operation(summary = "查询任务月历",
            description = "按月份查询当前用户可见任务在日历中的数量、逾期和完成统计。")
    @GetMapping("/month")
    public ApiResponse<TaskCalendarMonthResponse> month(@RequestParam String month) {
        return ApiResponse.success(taskCalendarService.month(YearMonth.parse(month)), MDC.get("traceId"));
    }

    @Operation(summary = "查询某日任务列表",
            description = "按日期查询当前用户可见的当天任务详情列表。")
    @GetMapping("/day")
    public ApiResponse<TaskCalendarDayResponse> day(@RequestParam String date) {
        return ApiResponse.success(taskCalendarService.day(LocalDate.parse(date)), MDC.get("traceId"));
    }
}
