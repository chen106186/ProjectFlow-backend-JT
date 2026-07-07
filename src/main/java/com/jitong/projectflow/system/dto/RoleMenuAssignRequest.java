package com.jitong.projectflow.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleMenuAssignRequest {
    private List<Long> menuIds = new ArrayList<>();
}
