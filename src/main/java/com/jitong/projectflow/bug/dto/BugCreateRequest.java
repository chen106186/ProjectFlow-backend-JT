package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugCreateRequest {
    @NotNull
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "任务 ID。")
    private Long taskId;
    @NotBlank
    @Schema(description = "标题。")
    private String title;
    @NotBlank
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @NotNull
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @NotBlank
    @Schema(description = "详细描述。")
    private String description;
    @NotBlank
    @Schema(description = "复现步骤。")
    private String reproduceSteps;
}
