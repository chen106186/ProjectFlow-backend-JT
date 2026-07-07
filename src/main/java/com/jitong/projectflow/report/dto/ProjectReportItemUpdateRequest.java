package com.jitong.projectflow.report.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportItemUpdateRequest {
    private String content;
    private Long ownerId;
    private String priority;
    private String status;
    private LocalDate plannedDate;
    private String description;
}
