package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String code;
    private String name;
}
