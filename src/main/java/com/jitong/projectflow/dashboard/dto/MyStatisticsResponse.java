package com.jitong.projectflow.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyStatisticsResponse {
    private long myTaskTotal;
    private long myTaskCompleted;
    private long myTaskOverdue;
    private long myBugTotal;
    private long myBugOpen;
    private long myRequirementTotal;
    private long myRequirementAccepted;
    private long unreadNoticeCount;
}
