package com.jitong.projectflow.bug.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugCreateRequest {
    @NotNull
    private Long projectId;
    private Long taskId;
    @NotBlank
    private String title;
    @NotBlank
    private String priority;
    @NotNull
    private Long assigneeId;
    @NotBlank
    private String description;
    @NotBlank
    private String reproduceSteps;
}
