package com.jitong.projectflow.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "我的工作台统计响应。")
public class MyStatisticsResponse {
    @Schema(description = "我的任务总数。")
    private long myTaskTotal;
    @Schema(description = "我的已完成任务数量。")
    private long myTaskCompleted;
    @Schema(description = "我的逾期任务数量。")
    private long myTaskOverdue;
    @Schema(description = "我的Bug总数。")
    private long myBugTotal;
    @Schema(description = "我的未关闭Bug数量。")
    private long myBugOpen;
    @Schema(description = "我的需求总数。")
    private long myRequirementTotal;
    @Schema(description = "我的已采纳需求数量。")
    private long myRequirementAccepted;
    @Schema(description = "未读通知数量。")
    private long unreadNoticeCount;
    @Schema(description = "我的任务状态分布，key为状态编码，value为数量。")
    private Map<String, Long> taskStatusDistribution;
    @Schema(description = "我的任务优先级分布，key为优先级编码，value为数量。")
    private Map<String, Long> taskPriorityDistribution;
    @Schema(description = "我的Bug状态分布，key为状态编码，value为数量。")
    private Map<String, Long> bugStatusDistribution;
    @Schema(description = "任务完成趋势，每天已完成任务数列表。")
    private List<TrendDataPoint> completionTrend;
    @Schema(description = "各项目完成任务分布，key为项目名称，value为已完成任务数。")
    private Map<String, Long> projectDistribution;
}
