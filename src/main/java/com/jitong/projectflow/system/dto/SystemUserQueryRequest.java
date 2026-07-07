package com.jitong.projectflow.system.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class SystemUserQueryRequest extends PageQuery {
    private String keyword;
    private Long departmentId;
    private Boolean enabled;
}
