package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleMenuAssignRequest {
    @Schema(description = "菜单 ID 列表。")
    private List<Long> menuIds = new ArrayList<>();
}
