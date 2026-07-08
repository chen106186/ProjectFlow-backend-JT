package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCreateRequest {
    @Schema(description = "父级节点 ID，根节点可为空或 0。")
    private Long parentId;
    @NotBlank
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "排序号，数值越小越靠前。")
    private Integer sortOrder;
}
