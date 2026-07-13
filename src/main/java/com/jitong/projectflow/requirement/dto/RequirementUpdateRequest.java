package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RequirementUpdateRequest {

    @Schema(description = "标题。")
    private String title;

    @Schema(description = "需求类型。")
    private String requirementType;

    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;

    @Schema(description = "项目 ID。")
    private Long projectId;

    @Schema(description = "审核人用户 ID。")
    private Long reviewerId;

    @Schema(description = "详细描述。")
    private String description;

    @Schema(description = "标签，多个标签可用逗号分隔。")
    private String tags;

    @Schema(description = "状态，可选，直接设置需求状态（管理员操作）。")
    private String status;

    public String title() { return title; }
    public String requirementType() { return requirementType; }
    public String priority() { return priority; }
    public Long projectId() { return projectId; }
    public Long reviewerId() { return reviewerId; }
    public String description() { return description; }
    public String tags() { return tags; }
    public String status() { return status; }

    public void setTitle(String title) { this.title = title; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(String tags) { this.tags = tags; }
    public void setStatus(String status) { this.status = status; }
}
