package com.jitong.projectflow.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GanttSummaryResponse {

    private int total;

    private int completed;

    private int overdue;

    private int dueSoon;

    private int overallProgress;
}
