package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectUpdateRequest {
    @Schema(description = "项目类型，例如管理类项目或执行类项目。")
    private String projectType;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "项目阶段。")
    private String stage;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "合同状态。")
    private String contractStatus;
    @Schema(description = "项目经理用户 ID。")
    private Long managerId;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;
    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;
    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;
    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;
}
