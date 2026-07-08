package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectReportItemResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目汇报 ID。")
    private Long reportId;
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
    @Schema(description = "创建人 ID。")
    private Long createdBy;
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;
    @Schema(description = "更新人 ID。")
    private Long updatedBy;
    @Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime updatedAt;
}
