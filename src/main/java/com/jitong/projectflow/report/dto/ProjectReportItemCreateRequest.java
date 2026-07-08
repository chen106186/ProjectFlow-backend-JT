package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportItemCreateRequest {
    @NotBlank
    @Schema(description = "内容。")
    private String content;
    @NotNull
    @Schema(description = "负责人用户 ID。")
    private Long ownerId;
    @NotBlank
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @NotBlank
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "计划汇报日期，格式 yyyy-MM-dd。")
    private LocalDate plannedDate;
    @Schema(description = "详细描述。")
    private String description;
}
