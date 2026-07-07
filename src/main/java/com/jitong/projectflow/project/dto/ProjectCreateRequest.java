package com.jitong.projectflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectCreateRequest {
    @NotBlank
    private String projectType;
    @NotBlank
    private String name;
    private String stage;
    private String status;
    private String contractStatus;
    private Long managerId;
    private String description;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
}
