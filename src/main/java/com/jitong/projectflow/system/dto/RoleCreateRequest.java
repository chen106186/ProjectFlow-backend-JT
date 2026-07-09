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
    @Schema(description = "描述。")
    private String description;
    @Schema(description = "是否启用，默认 true。")
    private Boolean enabled;
    @Schema(description = "排序，数值越小越靠前，默认 0。")
    private Integer sortOrder;
}
