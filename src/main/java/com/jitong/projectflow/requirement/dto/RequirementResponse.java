package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequirementResponse {

    @Schema(description = "主键 ID。")
    private Long id;

    @Schema(description = "需求序号，从 1 开始递增。")
    private Long requirementNo;

    @Schema(description = "项目 ID。")
    private Long projectId;

    @Schema(description = "项目名称。")
    private String projectName;

    @Schema(description = "审核人用户 ID。")
    private Long reviewerId;

    @Schema(description = "审核人姓名。")
    private String reviewerName;

    @Schema(description = "标题。")
    private String title;

    @Schema(description = "需求类型。")
    private String requirementType;

    @Schema(description = "状态。")
    private String status;

    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;

    @Schema(description = "详细描述。")
    private String description;

    @Schema(description = "标签，多个标签可用逗号分隔。")
    private String tags;

    @Schema(description = "创建人 ID。")
    private Long createdBy;

    @Schema(description = "创建人姓名。")
    private String creatorName;

    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;

    @Schema(description = "更新人 ID。")
    private Long updatedBy;

    @Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime updatedAt;
}
