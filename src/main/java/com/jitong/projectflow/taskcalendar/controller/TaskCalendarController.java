package com.jitong.projectflow.taskcalendar.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarDayResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarMonthResponse;
import com.jitong.projectflow.taskcalendar.service.TaskCalendarService;
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
public class TaskCalendarController {
    private final TaskCalendarService taskCalendarService;

    @GetMapping("/month")
    public ApiResponse<TaskCalendarMonthResponse> month(@RequestParam String month) {
        return ApiResponse.success(taskCalendarService.month(YearMonth.parse(month)), MDC.get("traceId"));
    }

    @GetMapping("/day")
    public ApiResponse<TaskCalendarDayResponse> day(@RequestParam String date) {
        return ApiResponse.success(taskCalendarService.day(LocalDate.parse(date)), MDC.get("traceId"));
    }
}
