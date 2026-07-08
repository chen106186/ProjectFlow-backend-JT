package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserEnabledUpdateRequest {
    @NotNull
    @Schema(description = "是否启用。")
    private Boolean enabled;
}
