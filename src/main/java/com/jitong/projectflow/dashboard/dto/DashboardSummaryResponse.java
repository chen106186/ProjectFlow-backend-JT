package com.jitong.projectflow.dashboard.dto;

public record DashboardSummaryResponse(
        long managementProjectCount,
        long executionProjectCount,
        long inProgressProjectCount,
        long completedProjectCount
) {
}
