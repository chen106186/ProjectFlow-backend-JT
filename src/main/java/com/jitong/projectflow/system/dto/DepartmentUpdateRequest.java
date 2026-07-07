package com.jitong.projectflow.system.dto;

import lombok.Data;

@Data
public class DepartmentUpdateRequest {
    private Long parentId;
    private String name;
    private Integer sortOrder;
}
