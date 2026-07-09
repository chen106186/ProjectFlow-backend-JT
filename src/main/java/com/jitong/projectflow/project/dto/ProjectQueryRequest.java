package com.jitong.projectflow.project.dto;

import com.jitong.projectflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProjectQueryRequest extends PageQuery {
    @Schema(description = "项目分类，例如管理类项目或执行类项目。")
    private String projectType;

    @Schema(description = "项目业务类型，例如数字化项目、信息化项目或科研项目。")
    private String projectBusinessType;

    @Schema(description = "状态。")
    private String status;

    @Schema(description = "合同状态。")
    private String contractStatus;

    @Schema(description = "项目经理用户 ID。")
    private Long managerId;

    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;

    @Schema(description = "项目阶段筛选。")
    private String stage;
}