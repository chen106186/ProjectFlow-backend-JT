package com.jitong.projectflow.bug.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BugCommentCreateRequest {
    @NotBlank
    private String content;
}
