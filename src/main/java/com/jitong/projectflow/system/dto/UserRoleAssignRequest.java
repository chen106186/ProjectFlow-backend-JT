package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserRoleAssignRequest {
    @Schema(description = "角色 ID 列表。")
    private List<Long> roleIds = new ArrayList<>();
}
