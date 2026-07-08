package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateRequest {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "任务角色名称。")
    private String roleName;
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
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "标签，多个标签可用逗号分隔。")
    private String tags;
    @Schema(description = "备注。")
    private String remark;
}
