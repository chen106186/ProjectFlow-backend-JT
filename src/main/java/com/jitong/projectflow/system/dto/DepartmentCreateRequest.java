package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCreateRequest {
    private Long parentId;
    @NotBlank
    private String name;
    private Integer sortOrder;
}
