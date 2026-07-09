package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "编码。")
    private String code;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "描述。")
    private String description;
    @Schema(description = "是否启用。")
    private Boolean enabled;
    @Schema(description = "排序。")
    private Integer sortOrder;
}
