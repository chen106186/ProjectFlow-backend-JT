package com.jitong.projectflow.requirement.dto;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class RequirementQueryRequest extends PageQuery {
    private Long projectId;
}
