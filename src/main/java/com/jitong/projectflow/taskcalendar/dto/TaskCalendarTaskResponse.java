package com.jitong.projectflow.taskcalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TaskCalendarTaskResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;
    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;
    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;
    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;
    @Schema(description = "逾期天数，未逾期时为 0。")
    private Integer overdueDays;
    @Schema(description = "距离计划结束日期的剩余天数。")
    private Integer remainingDays;
    @Schema(description = "所属项目名称。")
    private String projectName;
    @Schema(description = "负责人姓名。")
    private String assigneeName;
}
