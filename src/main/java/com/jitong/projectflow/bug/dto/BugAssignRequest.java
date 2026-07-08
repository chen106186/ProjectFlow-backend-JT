package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugAssignRequest {
    @NotNull
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "原因说明。")
    private String reason;
}
