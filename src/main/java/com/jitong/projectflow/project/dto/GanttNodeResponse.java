package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class GanttNodeResponse {

    @Schema(description = "主键 ID。")
    private Long id;

    @Schema(description = "项目 ID。")
    private Long projectId;

    @Schema(description = "父级节点 ID，根节点可为空或 0。")
    private Long parentId;

    @Schema(description = "甘特图节点名称。")
    private String nodeName;

    @Schema(description = "nodeType 字段。")
    private String nodeType;

    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;

    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;

    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;

    @Schema(description = "状态。")
    private String status;

    @Schema(description = "进度百分比，取值范围 0 到 100。")
    private Integer progressPercent;

    @Schema(description = "排序号，数值越小越靠前。")
    private Integer sortOrder;
}
