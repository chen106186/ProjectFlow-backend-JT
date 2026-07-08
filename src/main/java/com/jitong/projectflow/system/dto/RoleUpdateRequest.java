package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
public class RoleUpdateRequest {
    @Schema(description = "编码。")
    private String code;
    @Schema(description = "名称。")
    private String name;
}
