package com.jitong.projectflow.taskcalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.YearMonth;
import java.util.List;

@Data
@Builder
public class TaskCalendarMonthResponse {
    @Schema(description = "月份，格式 yyyy-MM。")
    private YearMonth month;
    @Schema(description = "月历中的每日任务统计列表。")
    private List<TaskCalendarDayResponse> days;
}
