package com.jitong.projectflow.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "首页项目统计响应。")
public record DashboardSummaryResponse(
        @Schema(description = "管理类项目数量。")
        long managementProjectCount,

        @Schema(description = "执行类项目数量。")
        long executionProjectCount,

        @Schema(description = "进行中项目数量。")
        long inProgressProjectCount,

        @Schema(description = "已完成项目数量。")
        long completedProjectCount) {
}
