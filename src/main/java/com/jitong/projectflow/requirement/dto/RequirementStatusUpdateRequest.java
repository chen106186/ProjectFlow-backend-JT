package com.jitong.projectflow.requirement.dto;

import jakarta.validation.constraints.NotBlank;

public class RequirementStatusUpdateRequest {

    @NotBlank
    private String status;

    public String status() { return status; }

    public void setStatus(String status) { this.status = status; }
}
