package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
public class BugUpdateRequest {
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
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "复现步骤。")
    private String reproduceSteps;
}
