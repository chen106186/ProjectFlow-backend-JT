package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BugResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "任务 ID。")
    private Long taskId;
    @Schema(description = "标题。")
    private String title;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "创建人 ID。")
    private Long creatorId;
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "复现步骤。")
    private String reproduceSteps;
    @Schema(description = "问题分析（开发填写）。")
    private String fixAnalysis;
    @Schema(description = "修复细节（开发填写）。")
    private String fixDetail;
    @Schema(description = "关闭时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime closedAt;
    @Schema(description = "创建时间。")
    private java.time.LocalDateTime createdAt;
    @Schema(description = "创建人姓名。")
    private String creatorName;
    @Schema(description = "负责人姓名。")
    private String assigneeName;
    @Schema(description = "项目名称。")
    private String projectName;
}
