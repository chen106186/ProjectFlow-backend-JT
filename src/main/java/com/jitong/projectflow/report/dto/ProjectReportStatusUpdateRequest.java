package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectReportStatusUpdateRequest {
    @NotBlank
    @Schema(description = "状态。")
    private String status;
}
