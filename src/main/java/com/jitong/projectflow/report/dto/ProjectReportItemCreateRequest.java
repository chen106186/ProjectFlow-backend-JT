package com.jitong.projectflow.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectReportItemCreateRequest {
    @NotBlank
    private String content;
    @NotNull
    private Long ownerId;
    @NotBlank
    private String priority;
    @NotBlank
    private String status;
    private LocalDate plannedDate;
    private String description;
}
