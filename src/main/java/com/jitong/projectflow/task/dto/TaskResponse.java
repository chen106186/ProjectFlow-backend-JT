package com.jitong.projectflow.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String roleName;
    private String priority;
    private String status;
    private Long assigneeId;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String description;
    private String tags;
    private String remark;
}
