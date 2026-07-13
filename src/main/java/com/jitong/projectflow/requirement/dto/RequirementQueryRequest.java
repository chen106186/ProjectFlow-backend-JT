package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class RequirementQueryRequest extends PageQuery {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "关键字，匹配标题或描述。")
    private String keyword;
    @Schema(description = "需求类型。")
    private String requirementType;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "状态，例如 PENDING_REVIEW、ACCEPTED、REJECTED。")
    private String status;
}
