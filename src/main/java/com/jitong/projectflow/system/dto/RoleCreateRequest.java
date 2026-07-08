package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleCreateRequest {
    @NotBlank
    @Schema(description = "编码。")
    private String code;
    @NotBlank
    @Schema(description = "名称。")
    private String name;
}
