package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "父级节点 ID，根节点可为空或 0。")
    private Long parentId;
    @Schema(description = "编码。")
    private String code;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "类型。")
    private String type;
    @Schema(description = "前端路由路径。")
    private String path;
    @Schema(description = "排序号，数值越小越靠前。")
    private Integer sortOrder;
}
