package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportUpdateRequest {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "标题。")
    private String title;
    @Schema(description = "汇报类型。")
    private String reportType;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "计划汇报日期，格式 yyyy-MM-dd。")
    private LocalDate plannedDate;
    @Schema(description = "实际汇报日期，格式 yyyy-MM-dd。")
    private LocalDate actualDate;
    @Schema(description = "汇报对象。")
    private String targetAudience;
    @Schema(description = "汇报地点或方式。")
    private String locationMethod;
    @Schema(description = "详细描述。")
    private String description;
}
