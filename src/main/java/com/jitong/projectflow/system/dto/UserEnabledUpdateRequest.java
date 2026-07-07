package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserEnabledUpdateRequest {
    @NotNull
    private Boolean enabled;
}
