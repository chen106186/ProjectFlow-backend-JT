package com.jitong.projectflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreateRequest {
    @NotNull
    private Long projectId;
    @NotBlank
    private String name;
    private String roleName;
    @NotBlank
    private String priority;
    @NotNull
    private Long assigneeId;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String description;
    private String tags;
    private String remark;
}
