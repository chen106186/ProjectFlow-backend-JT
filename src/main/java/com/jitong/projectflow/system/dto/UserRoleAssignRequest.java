package com.jitong.projectflow.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserRoleAssignRequest {
    private List<Long> roleIds = new ArrayList<>();
}
