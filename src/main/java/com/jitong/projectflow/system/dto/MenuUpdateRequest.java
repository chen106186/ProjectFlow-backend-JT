package com.jitong.projectflow.system.dto;

import lombok.Data;

@Data
public class MenuUpdateRequest {
    private Long parentId;
    private String code;
    private String name;
    private String type;
    private String path;
    private Integer sortOrder;
}
