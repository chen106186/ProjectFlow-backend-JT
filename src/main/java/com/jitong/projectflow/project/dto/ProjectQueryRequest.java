package com.jitong.projectflow.project.dto;

import lombok.Data;

@Data
public class ProjectQueryRequest {
    private String projectType;
    private String status;
    private String contractStatus;
    private Long managerId;
    private String keyword;
}
