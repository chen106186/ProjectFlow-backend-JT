package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Bug 统计汇总。")
public record BugSummaryResponse(
        @Schema(description = "Bug 总数。") long total,
        @Schema(description = "按状态统计。") Map<String, Long> byStatus,
        @Schema(description = "按优先级统计。") Map<String, Long> byPriority
) {
}
