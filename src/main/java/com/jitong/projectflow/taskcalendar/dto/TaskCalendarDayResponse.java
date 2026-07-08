package com.jitong.projectflow.taskcalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TaskCalendarDayResponse {
    @Schema(description = "日期，格式 yyyy-MM-dd。")
    private LocalDate date;
    @Schema(description = "总数量。")
    private int totalCount;
    @Schema(description = "当天任务列表。")
    private List<TaskCalendarTaskResponse> tasks;
}
