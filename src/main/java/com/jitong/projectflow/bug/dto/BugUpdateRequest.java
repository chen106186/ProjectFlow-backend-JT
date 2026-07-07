package com.jitong.projectflow.bug.dto;

import lombok.Data;

@Data
public class BugUpdateRequest {
    private Long projectId;
    private Long taskId;
    private String title;
    private String status;
    private String priority;
    private Long assigneeId;
    private String description;
    private String reproduceSteps;
}
