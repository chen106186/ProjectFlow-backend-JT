package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public class RequirementStatusUpdateRequest {

    @NotBlank
    @Schema(description = "状态。")
    private String status;

    public String status() { return status; }

    public void setStatus(String status) { this.status = status; }
}
