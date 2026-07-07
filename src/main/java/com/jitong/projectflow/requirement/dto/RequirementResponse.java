package com.jitong.projectflow.requirement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequirementResponse {

    private Long id;

    private Long projectId;

    private String title;

    private String requirementType;

    private String status;

    private String priority;

    private String description;

    private String tags;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
