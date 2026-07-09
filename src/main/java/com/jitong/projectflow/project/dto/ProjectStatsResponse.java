package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectStatsResponse(
        @Schema(description = "项目 ID。") Long projectId,
        @Schema(description = "任务总数。") long taskCount,
        @Schema(description = "Bug 总数。") long bugCount
) {}
