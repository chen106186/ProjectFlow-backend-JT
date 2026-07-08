package com.jitong.projectflow.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyStatisticsResponse {
    @Schema(description = "我的任务总数。")
    private long myTaskTotal;
    @Schema(description = "我的已完成任务数量。")
    private long myTaskCompleted;
    @Schema(description = "我的逾期任务数量。")
    private long myTaskOverdue;
    @Schema(description = "我的 Bug 总数。")
    private long myBugTotal;
    @Schema(description = "我的未关闭 Bug 数量。")
    private long myBugOpen;
    @Schema(description = "我的需求总数。")
    private long myRequirementTotal;
    @Schema(description = "我的已采纳需求数量。")
    private long myRequirementAccepted;
    @Schema(description = "未读通知数量。")
    private long unreadNoticeCount;
}
