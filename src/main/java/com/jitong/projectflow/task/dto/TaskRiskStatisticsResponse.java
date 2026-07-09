package com.jitong.projectflow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "任务风险统计响应。")
public class TaskRiskStatisticsResponse {
    @Schema(description = "总任务数。")
    private long total;
    @Schema(description = "已逾期数量（状态 OVERDUE）。")
    private long overdueCount;
    @Schema(description = "即将到期数量（状态 DUE_SOON）。")
    private long dueSoonCount;
    @Schema(description = "进行中数量（状态 IN_PROGRESS）。")
    private long inProgressCount;
    @Schema(description = "未开始数量（状态 NOT_STARTED）。")
    private long notStartedCount;
    @Schema(description = "已暂停数量（状态 PAUSED）。")
    private long pausedCount;
    @Schema(description = "已完成数量（状态 COMPLETED）。")
    private long completedCount;
}
