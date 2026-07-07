package com.jitong.projectflow.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String projectType;
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
