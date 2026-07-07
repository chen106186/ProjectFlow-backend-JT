package com.jitong.projectflow.bug.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BugAssignRequest {
    @NotNull
    private Long assigneeId;
    private String reason;
}
