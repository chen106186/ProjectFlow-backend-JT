package com.jitong.projectflow.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "首页项目统计响应。")
public record DashboardSummaryResponse(
        @Schema(description = "数字化项目数量。")
        long digitalizationProjectCount,

        @Schema(description = "信息化项目数量。")
        long informatizationProjectCount,

        @Schema(description = "科研项目数量。")
        long researchProjectCount,

        @Schema(description = "进行中项目数量。")
        long inProgressProjectCount,

        @Schema(description = "已完成项目数量。")
        long completedProjectCount) {
}
