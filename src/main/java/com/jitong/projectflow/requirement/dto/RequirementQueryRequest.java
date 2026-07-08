package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class RequirementQueryRequest extends PageQuery {
    @Schema(description = "项目 ID。")
    private Long projectId;
}
