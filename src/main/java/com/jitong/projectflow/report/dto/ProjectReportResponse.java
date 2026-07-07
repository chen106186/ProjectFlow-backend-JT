package com.jitong.projectflow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectReportResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String reportType;
    private String status;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private String targetAudience;
    private String locationMethod;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private List<ProjectReportItemResponse> items;
}
