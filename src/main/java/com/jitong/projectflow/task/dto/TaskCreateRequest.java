package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "任务创建请求。")
public class TaskCreateRequest {
    @NotNull
    @Schema(description = "项目 ID。")
    private Long projectId;

    @Schema(description = "父任务 ID，顶级任务为空。")
    private Long parentId;

    @NotBlank
    @Schema(description = "任务名称。")
    private String name;

    @Schema(description = "任务角色名称，例如开发、测试、设计。")
    private String roleName;

    @NotBlank
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;

    @NotNull
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;

    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;

    @Schema(description = "详细描述。")
    private String description;

    @Schema(description = "标签，多个标签可用逗号分隔。")
    private String tags;

    @Schema(description = "备注。")
    private String remark;

    @Schema(description = "同级任务排序号。")
    private Integer sortOrder;
}
