package com.jitong.projectflow.task.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskQueryRequest {
    private Long projectId;
    private Long assigneeId;
    private String priority;
    private String status;
    private String keyword;
    private LocalDate plannedEndDate;
}
