package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
}
