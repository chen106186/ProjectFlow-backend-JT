package com.jitong.projectflow.bug.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BugResponse {
    private Long id;
    private Long projectId;
    private Long taskId;
    private String title;
    private String status;
    private String priority;
    private Long creatorId;
    private Long assigneeId;
    private String description;
    private String reproduceSteps;
    private LocalDateTime closedAt;
}
