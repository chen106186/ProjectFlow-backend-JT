package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentResponse {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sortOrder;
}
