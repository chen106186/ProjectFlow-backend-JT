package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BugCommentCreateRequest {
    @NotBlank
    @Schema(description = "内容。")
    private String content;
}
