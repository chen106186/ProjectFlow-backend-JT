package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GanttSummaryResponse {

    @Schema(description = "总记录数。")
    private int total;

    @Schema(description = "是否完成。")
    private int completed;

    @Schema(description = "overdue 字段。")
    private int overdue;

    @Schema(description = "dueSoon 字段。")
    private int dueSoon;

    @Schema(description = "整体进度百分比。")
    private int overallProgress;

    @Schema(description = "总任务数（用于进度计算）。")
    private long totalTaskCount;

    @Schema(description = "已完成任务数（用于进度计算）。")
    private long completedTaskCount;
}
