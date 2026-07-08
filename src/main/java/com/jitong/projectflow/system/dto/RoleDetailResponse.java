package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleDetailResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "编码。")
    private String code;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "菜单 ID 列表。")
    private List<Long> menuIds;
}
