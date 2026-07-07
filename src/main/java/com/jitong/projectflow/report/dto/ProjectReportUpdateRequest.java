package com.jitong.projectflow.report.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportUpdateRequest {
    private Long projectId;
    private String title;
    private String reportType;
    private String status;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private String targetAudience;
    private String locationMethod;
    private String description;
}
