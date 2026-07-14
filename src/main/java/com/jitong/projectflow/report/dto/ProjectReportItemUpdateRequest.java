package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportItemUpdateRequest {
    @Schema(description = "内容。")
    private String content;
    @Schema(description = "负责人用户 ID。")
    private Long ownerId;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "计划汇报日期，格式 yyyy-MM-dd。")
    private LocalDate plannedDate;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "关联任务 ID。")
    private Long relatedTaskId;
}
