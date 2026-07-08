package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskQueryRequest extends PageQuery {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;
}
