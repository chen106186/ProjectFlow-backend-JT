package com.jitong.projectflow.project.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectNodeUpdateRequest {

    private String nodeName;

    private String status;

    private Integer progressPercent;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;
}
