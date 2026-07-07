package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuResponse {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String type;
    private String path;
    private Integer sortOrder;
}
