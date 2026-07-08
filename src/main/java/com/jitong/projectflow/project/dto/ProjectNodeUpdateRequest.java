package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectNodeUpdateRequest {

    @Schema(description = "甘特图节点名称。")
    private String nodeName;

    @Schema(description = "状态。")
    private String status;

    @Schema(description = "进度百分比，取值范围 0 到 100。")
    private Integer progressPercent;

    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;

    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;

    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;
}
