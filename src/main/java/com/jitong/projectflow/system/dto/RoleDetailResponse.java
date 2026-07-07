package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleDetailResponse {
    private Long id;
    private String code;
    private String name;
    private List<Long> menuIds;
}
