package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuCreateRequest {
    private Long parentId;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    private String path;
    private Integer sortOrder;
}
