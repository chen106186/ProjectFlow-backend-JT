package com.jitong.projectflow.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportCreateRequest {
    @NotNull
    private Long projectId;
    @NotBlank
    private String title;
    @NotBlank
    private String reportType;
    private String status;
    @NotNull
    private LocalDate plannedDate;
    private LocalDate actualDate;
    @NotBlank
    private String targetAudience;
    @NotBlank
    private String locationMethod;
    @NotBlank
    private String description;
}
