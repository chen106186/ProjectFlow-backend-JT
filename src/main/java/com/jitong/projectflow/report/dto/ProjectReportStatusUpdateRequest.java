package com.jitong.projectflow.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectReportStatusUpdateRequest {
    @NotBlank
    private String status;
}
