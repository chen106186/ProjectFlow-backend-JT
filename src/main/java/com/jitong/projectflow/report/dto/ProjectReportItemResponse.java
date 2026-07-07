package com.jitong.projectflow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectReportItemResponse {
    private Long id;
    private Long reportId;
    private String content;
    private Long ownerId;
    private String priority;
    private String status;
    private LocalDate plannedDate;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
